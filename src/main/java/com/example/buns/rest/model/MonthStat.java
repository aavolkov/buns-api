package com.example.buns.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * MonthStat.
 *
 * @author avolkov
 */

@Data
@AllArgsConstructor
public class MonthStat {

    private String month;

    private int count;

}
