/*
 *   (C) Copyright 2009-2010 hSenid Software International (Pvt) Limited.
 *   All Rights Reserved.
 *
 *   These materials are unpublished, proprietary, confidential source code of
 *   hSenid Software International (Pvt) Limited and constitute a TRADE SECRET
 *   of hSenid Software International (Pvt) Limited.
 *
 *   hSenid Software International (Pvt) Limited retains all title to and intellectual
 *   property rights in these materials.
 */
package jk.ashes.util;

/**
 * $LastChangedDate$
* $LastChangedBy$
* $LastChangedRevision$
*/
public class Range implements Comparable {
    private long lower;
    private long upper;

    public Range(long lower, long upper) {
        if (lower > upper) {

        }
        this.lower = lower;
        this.upper = upper;
    }

    public Range(long i) {
        lower = upper = i;
    }

    public int compareTo(Object o) throws ClassCastException {
        Range other = (Range) o;
        if (upper < other.lower) {
            return -1;
        }
        if (lower > other.upper) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Range)) return false;

        Range range = (Range) o;

        if (upper < range.lower) {
            return false;
        }
        if (lower > range.upper) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
