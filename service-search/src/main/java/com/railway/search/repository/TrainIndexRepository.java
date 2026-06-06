package com.railway.search.repository;

import com.railway.search.index.TrainIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data ES Repository：自动生成 CRUD + 简单派生查询。
 * 复杂查询走 {@link org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate}。
 */
public interface TrainIndexRepository extends ElasticsearchRepository<TrainIndex, String> {
}
