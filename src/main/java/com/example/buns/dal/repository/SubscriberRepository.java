package com.example.buns.dal.repository;

import com.example.buns.dal.entity.SubscriberDal;
import com.example.buns.dal.entity.TypeSubscribe;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * UserRepository.
 *
 * @author avolkov
 */

@Repository
public interface SubscriberRepository extends CrudRepository<SubscriberDal, Long>,
        JpaSpecificationExecutor<SubscriberDal> {

    List<SubscriberDal> findByTelegramIdAndTypeSubscribe(String chatId, TypeSubscribe type);

    List<SubscriberDal> findAllByTelegramId(String chatId);

    List<SubscriberDal> findAllByTypeSubscribe(TypeSubscribe typeSubscribe);

    @Query("select s from SubscriberDal s where s.finishDate <= CURRENT_TIMESTAMP")
    List<SubscriberDal> findAllExpired();

}
