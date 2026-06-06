package com.railway.trainstock.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预占成功后返回：剩余库存 + 使用的 Redis key。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupyVO {

    private String stockKey;
    private Long remainAfter;
}
