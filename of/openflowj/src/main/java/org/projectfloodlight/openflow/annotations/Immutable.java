package org.projectfloodlight.openflow.annotations;

/**
 * This annotation marks a class that is considered externally immutable. I.e.,
 * the externally visible state of the class will not change after its
 * construction. Such a class can be freely shared between threads and does not
 * require defensive copying (don't call clone).
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 */
public @interface Immutable {

}
