package com.cgi.kinota.persistence.cassandra.domain.support;

import java.util.Date;

/**
 * Created by bmiles on 6/27/17.
 */
public class DatastreamTemporalSummary {
    private Date phenomenonTimeBegin;
    private Date phenomenonTimeEnd;
    private Date resultTimeBegin;
    private Date resultTimeEnd;

    public DatastreamTemporalSummary() {}

    public DatastreamTemporalSummary(Date phenomenonTimeBegin, Date phenomenonTimeEnd, Date resultTimeBegin, Date resultTimeEnd) {
        this.phenomenonTimeBegin = phenomenonTimeBegin;
        this.phenomenonTimeEnd = phenomenonTimeEnd;
        this.resultTimeBegin = resultTimeBegin;
        this.resultTimeEnd = resultTimeEnd;
    }

    public Date getPhenomenonTimeBegin() {
        return phenomenonTimeBegin;
    }

    public void setPhenomenonTimeBegin(Date phenomenonTimeBegin) {
        this.phenomenonTimeBegin = phenomenonTimeBegin;
    }

    public Date getPhenomenonTimeEnd() {
        return phenomenonTimeEnd;
    }

    public void setPhenomenonTimeEnd(Date phenomenonTimeEnd) {
        this.phenomenonTimeEnd = phenomenonTimeEnd;
    }

    public Date getResultTimeBegin() {
        return resultTimeBegin;
    }

    public void setResultTimeBegin(Date resultTimeBegin) {
        this.resultTimeBegin = resultTimeBegin;
    }

    public Date getResultTimeEnd() {
        return resultTimeEnd;
    }

    public void setResultTimeEnd(Date resultTimeEnd) {
        this.resultTimeEnd = resultTimeEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatastreamTemporalSummary)) return false;

        DatastreamTemporalSummary that = (DatastreamTemporalSummary) o;

        if (phenomenonTimeBegin != null ? !phenomenonTimeBegin.equals(that.phenomenonTimeBegin) : that.phenomenonTimeBegin != null)
            return false;
        if (phenomenonTimeEnd != null ? !phenomenonTimeEnd.equals(that.phenomenonTimeEnd) : that.phenomenonTimeEnd != null)
            return false;
        if (resultTimeBegin != null ? !resultTimeBegin.equals(that.resultTimeBegin) : that.resultTimeBegin != null)
            return false;
        return resultTimeEnd != null ? resultTimeEnd.equals(that.resultTimeEnd) : that.resultTimeEnd == null;
    }

    @Override
    public int hashCode() {
        int result = phenomenonTimeBegin != null ? phenomenonTimeBegin.hashCode() : 0;
        result = 31 * result + (phenomenonTimeEnd != null ? phenomenonTimeEnd.hashCode() : 0);
        result = 31 * result + (resultTimeBegin != null ? resultTimeBegin.hashCode() : 0);
        result = 31 * result + (resultTimeEnd != null ? resultTimeEnd.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DatastreamTemporalSummary{" +
                "phenomenonTimeBegin=" + phenomenonTimeBegin +
                ", phenomenonTimeEnd=" + phenomenonTimeEnd +
                ", resultTimeBegin=" + resultTimeBegin +
                ", resultTimeEnd=" + resultTimeEnd +
                '}';
    }
}
