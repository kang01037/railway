package com.railway.search.service.impl;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.model.R;
import com.railway.search.feign.TrainStockFeignClient;
import com.railway.search.feign.dto.SeatRemainVO;
import com.railway.search.index.TrainIndex;
import com.railway.search.repository.TrainIndexRepository;
import com.railway.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检索服务实现。
 *
 * <h3>ES 查询策略</h3>
 * <ul>
 *   <li>用 {@link CriteriaQuery}（基于 Lucene Criteria）拼装：startStation / endStation / runDate。</li>
 *   <li>不依赖 IK 分词器（demo 阶段 ES 可能未装 ik）→ 退化为 wildcard 模糊匹配（contains）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final TrainIndexRepository trainIndexRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final TrainStockFeignClient trainStockFeignClient;

    @Override
    public List<TrainIndex> searchTrains(String from, String to, LocalDate date) {
        Criteria criteria = new Criteria();
        boolean any = false;
        if (from != null && !from.isBlank()) {
            criteria = criteria.and(new Criteria("startStation").contains(from));
            any = true;
        }
        if (to != null && !to.isBlank()) {
            criteria = criteria.and(new Criteria("endStation").contains(to));
            any = true;
        }
        if (date != null) {
            criteria = criteria.and(new Criteria("runDate").is(date.toString()));
            any = true;
        }
        if (!any) {
            // 无过滤条件 → 直接查前 50 条
            return trainIndexRepository.findAll(PageRequest.of(0, 50)).getContent();
        }
        CriteriaQuery query = new CriteriaQuery(criteria).setPageable(PageRequest.of(0, 50));
        SearchHits<TrainIndex> hits = elasticsearchOperations.search(query, TrainIndex.class);
        return hits.getSearchHits().stream().map(h -> h.getContent()).toList();
    }

    @Override
    public Map<String, SeatRemainVO> getSeats(String trainNo, LocalDate date) {
        if (trainNo == null || trainNo.isBlank() || date == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "trainNo / runDate 必填");
        }
        R<Map<String, SeatRemainVO>> r = trainStockFeignClient.getSeats(trainNo, date.toString());
        if (r == null || !r.isSuccess() || r.getData() == null) {
            log.warn("train-stock Feign 拿座位余票失败 trainNo={} date={} msg={}", trainNo, date, r);
            return Map.of();
        }
        return r.getData();
    }
}
