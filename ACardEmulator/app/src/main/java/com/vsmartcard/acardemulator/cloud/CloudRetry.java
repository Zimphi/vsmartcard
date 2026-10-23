// SPDX-License-Identifier: GPL-3.0-or-later
// Phoenix Cloud extension; license notice added 2026-10-23. See LICENSE_REVIEW.md.
package com.vsmartcard.acardemulator.cloud;
/** Monotonic retry budget for connection checks; never used to replay APDUs. */
final class CloudRetry {
    private int failures;
    private long retryAt;
    synchronized boolean ready(long now){return now>=retryAt;}
    synchronized void failed(long now){failures=Math.min(5,failures+1);retryAt=now+Math.min(15000,1000L<<(failures-1));}
    synchronized void reset(){failures=0;retryAt=0;}
}
