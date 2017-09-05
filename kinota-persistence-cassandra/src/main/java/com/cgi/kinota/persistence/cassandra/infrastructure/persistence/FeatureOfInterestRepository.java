package com.cgi.kinota.persistence.cassandra.infrastructure.persistence;

import com.cgi.kinota.persistence.cassandra.infrastructure.persistence.CassandraDataRepository;
import com.cgi.kinota.persistence.cassandra.domain.FeatureOfInterest;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

import static com.cgi.kinota.commons.Constants.MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION;

/**
 * Created by bmiles on 2/27/17.
 */
public interface FeatureOfInterestRepository extends CassandraDataRepository<FeatureOfInterest> {
    // Note: this results in a NullPointerException.
    // Use FeatureOfInterestLocationNativeRepository.findFeatureOfInterestWithLocation instead.
    @Query("SELECT id FROM " + MATERIALIZED_VIEW_FEATURE_OF_INTEREST_LOCATION + " WHERE location = ?0")
    UUID findFeatureOfInterestWithLocation(@Param("location") String location);
}
