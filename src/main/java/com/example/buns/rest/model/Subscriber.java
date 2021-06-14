package com.example.buns.rest.model;

import com.example.buns.dal.entity.TypeSubscribe;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * EventDal.
 *
 * @author avolkov
 */

@Data
public class Subscriber {

    private Long id;

    private LocalDateTime startDate;

    private LocalDateTime finishDate;

    private String name;

    private String login;

    private String telegramId;

    private TypeSubscribe typeSubscribe;
}
