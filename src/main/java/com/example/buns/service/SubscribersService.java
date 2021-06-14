package com.example.buns.service;

import com.example.buns.dal.entity.SubscriberDal;
import com.example.buns.dal.entity.TypeSubscribe;
import com.example.buns.dal.repository.SubscriberRepository;
import com.example.buns.rest.model.MonthStat;
import com.example.buns.rest.model.Subscriber;
import lombok.Data;
import ma.glasnost.orika.MapperFacade;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

/**
 * SubscribersService.
 *
 * @author avolkov
 */

@Service
@Data
public class SubscribersService {

    private final SubscriberRepository subscriberRepository;
    private final MapperFacade mapperFacade;


    public List<Subscriber> getAll() {
        return mapperFacade.mapAsList(StreamSupport.stream(subscriberRepository.findAll().spliterator(), false)
                .collect(Collectors.toList()), Subscriber.class);
    }

    public Subscriber add(Subscriber subscriber) {
        SubscriberDal subscriberDal = new SubscriberDal();
        subscriberDal.setTelegramId(subscriber.getTelegramId());
        subscriberDal.setName(subscriber.getName());
        subscriberDal.setLogin(subscriber.getLogin());
        subscriberDal.setStartDate(subscriber.getStartDate());
        subscriberDal.setTypeSubscribe(subscriber.getTypeSubscribe());
        subscriberDal.setEnable(true);

        if (subscriber.getTypeSubscribe().equals(TypeSubscribe.DEMO)) {
            subscriberDal.setFinishDate(subscriber.getStartDate().plusDays(3));
        } else if (subscriber.getTypeSubscribe().equals(TypeSubscribe.FULL)) {
            subscriberDal.setFinishDate(subscriber.getStartDate().plusDays(30));
        }

        subscriberDal = subscriberRepository.save(subscriberDal);

        return mapperFacade.map(subscriberDal, Subscriber.class);
    }

    public List<MonthStat> getStat() {
        List<SubscriberDal> data = subscriberRepository.findAllByTypeSubscribe(TypeSubscribe.FULL);

        Map<String, List<SubscriberDal>> stats = data.stream().collect(groupingBy(item -> item.getStartDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.US)));

        return stats.entrySet().stream().map(entry -> new MonthStat(entry.getKey(), entry.getValue().size())).collect(Collectors.toList());
    }

    public Subscriber update(Subscriber subscriber) {
        Optional<SubscriberDal> subscriberOld = subscriberRepository.findById(subscriber.getId());

        return mapperFacade.map(subscriberRepository.save(mapperFacade.map(subscriber, SubscriberDal.class)), Subscriber.class);
    }

    public void disable(Long id) throws Exception {

        Optional<SubscriberDal> subscriber = subscriberRepository.findById(id);

        if (!subscriber.isPresent()) {
            throw new Exception("Subscriber not found");
        }

        subscriber.get().setEnable(false);

        subscriberRepository.save(subscriber.get());
    }

    @Transactional
    public List<Subscriber> getExpired() {
        return mapperFacade.mapAsList(subscriberRepository.findAllExpired(), Subscriber.class);
    }

    public void remove(Long id) {
        subscriberRepository.deleteById(id);
    }

    public boolean isDemoAccess(String chatId) {
        List<SubscriberDal> result = subscriberRepository.findByTelegramIdAndTypeSubscribe(chatId, TypeSubscribe.DEMO);

        return result == null || result.isEmpty();
    }
}
