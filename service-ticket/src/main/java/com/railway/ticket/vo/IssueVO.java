package com.railway.ticket.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 出票响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueVO {

    private String orderNo;
    private Integer ticketCount;
    private List<String> ticketNos;
}
