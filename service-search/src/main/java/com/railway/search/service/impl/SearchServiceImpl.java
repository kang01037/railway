package com.railway.search.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.model.R;
import com.railway.search.feign.TrainStockFeignClient;
import com.railway.search.feign.dto.SeatRemainVO;
import com.railway.search.index.TrainIndex;
import com.railway.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 检索服务实现。
 *
 * <h3>ES 查询策略</h3>
 * <ul>
 *   <li>用 {@link NativeQuery} 拼装 {@code bool query}：startStation / endStation 用 {@code match}（标准分词器下中文单字分词也能命中），runDate 用 {@code term}。</li>
 *   <li>不依赖 IK 分词器。</li>
 * </ul>
 *
 * <h3>ES 软依赖</h3>
 * {@link ElasticsearchOperations} 注入为 {@code required=false}，ES 不可用时（未启动 / 版本不兼容）
 * {@code searchTrains} 返回空集并打 warn 日志。Feign 查座位（{@code getSeats}）不依赖 ES。
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    private final TrainStockFeignClient trainStockFeignClient;

    public SearchServiceImpl(TrainStockFeignClient trainStockFeignClient) {
        this.trainStockFeignClient = trainStockFeignClient;
    }

    @Override
    public List<TrainIndex> searchTrains(String from, String to, LocalDate date) {
        if (elasticsearchOperations == null) {
            log.warn("ES 不可用，searchTrains 降级返回空集 from={} to={} date={}", from, to, date);
            return List.of();
        }
        try {
            BoolQuery.Builder bool = new BoolQuery.Builder();
            boolean any = false;
            if (from != null && !from.isBlank()) {
                bool.must(Query.of(q -> q.match(m -> m.field("startStation").query(from))));
                any = true;
            }
            if (to != null && !to.isBlank()) {
                bool.must(Query.of(q -> q.match(m -> m.field("endStation").query(to))));
                any = true;
            }
            if (date != null) {
                bool.must(Query.of(q -> q.term(t -> t.field("runDate").value(date.toString()))));
                any = true;
            }
            if (!any) {
                bool.must(Query.of(q -> q.matchAll(m -> m)));
            }
            NativeQuery query = NativeQuery.builder()
                    .withQuery(bool.build()._toQuery())
                    .withPageable(PageRequest.of(0, 50))
                    .build();
            SearchHits<TrainIndex> hits = elasticsearchOperations.search(query, TrainIndex.class);
            return hits.getSearchHits().stream().map(h -> h.getContent()).toList();
        } catch (Exception e) {
            log.warn("ES 查询失败，降级返回空集 from={} to={} date={} err={}", from, to, date, e.toString());
            return List.of();
        }
    }

    @Override
    public List<SeatRemainVO> getSeats(String trainNo, LocalDate date) {
        if (trainNo == null || trainNo.isBlank() || date == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "trainNo / runDate 必填");
        }
        R<List<SeatRemainVO>> r = trainStockFeignClient.getSeats(trainNo, date.toString());
        if (r == null || !r.isSuccess() || r.getData() == null) {
            log.warn("train-stock Feign 拿座位余票失败 trainNo={} date={} msg={}", trainNo, date, r);
            return List.of();
        }
        return r.getData();
    }
}
