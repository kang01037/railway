package com.railway.search.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车次 ES 文档（{@code train_index}）。
 *
 * <p>字段映射：trainNo 用 keyword（精确）；station 字段 text + keyword（既能模糊又能聚合）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "train_index")
public class TrainIndex {

    @Id
    private String id;                              // 用 trainNo 作 doc id

    @Field(type = FieldType.Keyword)
    private String trainNo;

    @Field(type = FieldType.Keyword)
    private String trainType;                       // G/D/K/T

    @Field(type = FieldType.Text, analyzer = "standard")
    private String startStation;

    @Field(type = FieldType.Keyword)
    private String startStationKeyword;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String endStation;

    @Field(type = FieldType.Keyword)
    private String endStationKeyword;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime startTime;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime endTime;

    @Field(type = FieldType.Keyword)
    private String runDays;                         // "1111111"

    /** 适用日期：用于按 date 过滤（demo 简化为 single date，可换 nested date range） */
    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate runDate;

    /** 各座位类型基础票价（demo 简化：单层 flat，不 nested） */
    @Field(type = FieldType.Object)
    private List<SeatPrice> prices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatPrice {
        @Field(type = FieldType.Keyword)
        private String seatType;                    // BUSINESS/FIRST/SECOND/STAND
        @Field(type = FieldType.Double)
        private Double price;
    }
}
