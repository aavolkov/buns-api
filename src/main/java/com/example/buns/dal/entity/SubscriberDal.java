package com.example.buns.dal.entity;

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

@Entity
@Data
@Table(name = "subscriber")
public class SubscriberDal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_subscribe")
    private LocalDateTime startDate;

    @Column(name = "end_subscribe")
    private LocalDateTime finishDate;

    @Column(name = "name")
    private String name;

    @Column(name = "login")
    private String login;

    @Column(name = "telegram_id")
    private String telegramId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_subscribe")
    private TypeSubscribe typeSubscribe;

    private Boolean enable;
}
