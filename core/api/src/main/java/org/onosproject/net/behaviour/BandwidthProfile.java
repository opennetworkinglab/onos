/*
 * Copyright 2016-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onlab.packet.DscpClass;
import org.onlab.util.Bandwidth;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.Meter;
import static org.onosproject.net.behaviour.BandwidthProfileAction.Action;

import java.util.Iterator;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of a generic bandwidth profile (marker/policer).
 */
@Beta
public final class BandwidthProfile {

    /**
     * Denotes the type of the bandwidth profile.
     */
    enum Type {
        /**
         * Corresponds to a Single Rate Two Color Marker/Policer.
         */
        sr2CM,

        /**
         * Corresponds to a Single Rate Three Color Marker/Policer.
         * (IETF RFC 2697)
         */
        srTCM,

        /**
         * Corresponds to a Two Rate Three Color Marker/Policer.
         * (IETF RFC 2698)
         */
        trTCM
    }

    private final String name;
    private final Bandwidth cir;
    private final Bandwidth pir;
    private final Integer cbs;
    private final Integer pbs;
    private final Integer ebs;
    private final BandwidthProfileAction greenAction;
    private final BandwidthProfileAction yellowAction;
    private final BandwidthProfileAction redAction;
    private final boolean colorAware;

    /**
     * BandwidthProfile constructor.
     *
     * @param name the profile name
     * @param cir the committed information rate (CIR)
     * @param cbs the committed burst size (CBS) measured in bytes
     * @param pir the peak information rate (PIR)
     * @param pbs the peak burst size (PBS) measured in bytes
     * @param greenAction the action to be taken for traffic that conforms
     *                    to the CIR/CBS
     * @param yellowAction srTCM: the action to be taken for traffic that
     *                     conforms to the EBS but not to the CIR/CBS
     *                     trTCM: the action to be taken for traffic that
     *                     conforms to the PIR/PBS but not to the CIR/CBS
     * @param redAction sr2CM: the action to be taken for traffic that
     *                  does not conform to the CIR/CBS
     *                  srTCM: the action to be taken for traffic that
     *                  does not conform to the EBS
     *                  trTCM: the action to be taken for traffic that
     *                  does not conform to the PIR/PBS
     * @param colorAware indicates whether the profile considers incoming
     *                   traffic as already colored
     */
    private BandwidthProfile(String name,
                             Bandwidth cir, Bandwidth pir,
                             Integer cbs, Integer pbs, Integer ebs,
                             BandwidthProfileAction greenAction,
                             BandwidthProfileAction yellowAction,
                             BandwidthProfileAction redAction,
                             boolean colorAware) {
        this.name = name;
        this.cir = cir;
        this.pir = pir;
        this.cbs = cbs;
        this.pbs = pbs;
        this.ebs = ebs;
        this.greenAction = greenAction;
        this.yellowAction = yellowAction;
        this.redAction = redAction;
        this.colorAware = colorAware;
    }

    /**
     * Obtains the name of this bandwidth profile.
     *
     * @return the bandwidth profile name
     */
    public String name() {
        return name;
    }

    /**
     * Obtains the committed information rate (CIR) of this bandwidth profile.
     *
     * @return the CIR of the bandwidth profile
     */
    public Bandwidth cir() {
        return cir;
    }

    /**
     * Obtains the peak information rate (PIR) of this bandwidth profile.
     * If this profile does not have a PIR, null is returned.
     *
     * @return the PIR of the profile; null if the profile does not have a PIR
     */
    public Bandwidth pir() {
        return pir;
    }

    /**
     * Obtains the committed burst size (CBS) of this bandwidth profile.
     * The CBS is measured in bytes.
     * If this profile does not have a CBS, null is returned.
     *
     * @return the CBS of the profile (bytes);
     * null if the profile does not have a CBS
     */
    public Integer cbs() {
        return cbs;
    }

    /**
     * Obtains the peak burst size (PBS) of this bandwidth profile.
     * The PBS is measured in bytes.
     * If this profile does not have a PBS, null is returned.
     *
     * @return the PBS of the bandwidth profile (bytes);
     * null if the profile does not have a PBS
     */
    public Integer pbs() {
        return pbs;
    }

    /**
     * Obtains the excess burst size (EBS) of this bandwidth profile.
     * The EBS is measured in bytes.
     * If this profile does not have an EBS, null is returned.
     *
     * @return the EBS of the bandwidth profile (bytes);
     * null if the profile does not have an EBS
     */
    public Integer ebs() {
        return ebs;
    }

    /**
     * Obtains the action to be taken for traffic marked as green.
     * Green color marking is applied to traffic that conforms to the CIR/CBS.
     *
     * @return the action to be taken for traffic marked as green
     */
    public BandwidthProfileAction greenAction() {
        return greenAction;
    }

    /**
     * Obtains the action to be taken for traffic marked as yellow.
     * Yellow color marking is applied to traffic that does not conform
     * to the CIR/CBS but conforms to one of:
     * <ul>
     *     <li>EBS (srTCM type)</li>
     *     <li>PIR/PBS (trTCM type)</li>
     * </ul>
     * If this profile does has neither EBS or PIR/PBS, null is returned.
     *
     * @return the action to be taken for traffic marked as yellow;
     * null if neither EBS nor PIR/PBS are defined
     */
    public BandwidthProfileAction yellowAction() {
        return yellowAction;
    }

    /**
     * Obtains the action to be taken for traffic marked as red.
     * Red color marking is applied to traffic that does not conform
     * to one of the following:
     * <ul>
     *     <li>CIR/CBS (sr2CM type)</li>
     *     <li>EBS (srTCM type)</li>
     *     <li>PIR/PBS (trTCM type)</li>
     * </ul>
     *
     * @return the action to be taken for traffic marked as red
     */
    public BandwidthProfileAction redAction() {
        return redAction;
    }

    /**
     * Obtains the color-aware mode of the bandwidth profile.
     *
     * @return true if the bandwidth profile is color-aware; false otherwise
     */
    public boolean colorAware() {
        return colorAware;
    }

    /**
     * Obtains the bandwidth profile type depending on the profile parameters.
     * <ul>
     *     <li>When PIR is defined, the profile corresponds to a
     *     Two Rate Three Color Marker (trTCM)</li>
     *     <li>When EBS is defined, the profile corresponds to a
     *     Single Rate Three Color Marker (srTCM)</li>
     *     <li>When neither PIR nor EBS are defined, the profile corresponds to a
     *     Single Rate Two Color Marker/Policer (sr2CM)</li>
     * </ul>
     *
     * @return the bandwidth profile type
     */
    public Type type() {
        return pir != null ? Type.trTCM :
                ebs != null ? Type.srTCM : Type.sr2CM;
    }

    /**
     * Creates a bandwidth profile based on the parameters of a Meter.
     * NOTE: The dropPrecedence in the Meter is interpreted as
     * the DSCP class to set on the packet
     *
     * @param meter the Meter to be used for creating the bandwidth profile
     * @return the bandwidth profile created
     */
    public static BandwidthProfile fromMeter(Meter meter) {

        checkNotNull(meter);
        checkArgument(meter.bands().size() <= 2,
                      "Meter must have no more than two bands.");

        Iterator<Band> bandIterator = meter.bands().iterator();
        Band bandOne = bandIterator.next();
        Band bandTwo = bandIterator.hasNext() ? bandIterator.next() : null;

        // Assign values to yellowBand and redBand depending on
        // the number of bands in the meter.
        // If only one band exists it will be designated as the redBand.
        // If two bands exist, the one with the lower rate will be
        // the yellowBand and the other the redBand.
        Band yellowBand = (bandTwo == null ? null :
                bandTwo.rate() > bandOne.rate() ? bandOne : bandTwo);
        Band redBand = (bandTwo == null ? bandOne :
                yellowBand == bandOne ? bandTwo : bandOne);

        BandwidthProfile.Builder bandwidthProfileBuilder = new Builder()
                // Consider the meter id as the bandwidth profile name
                .name(meter.id().toString())
                .colorAware(false)
                // The implicit green action is pass
                .greenAction(getBuilder(Action.PASS).build());

        if (yellowBand != null) {
            // Try to add yellow action; CIR/CBS will be obtained from
            // yellowBand and PIR/PBS from redBand.
            BandwidthProfileAction yellowAction =
                    getBwProfileActionFromBand(yellowBand);
            checkNotNull(yellowAction,
                         "Could not obtain yellow action from meter band");
            bandwidthProfileBuilder
                    .cir(Bandwidth.kBps(yellowBand.rate()))
                    .cbs(yellowBand.burst() == null ? null :
                                 yellowBand.burst().intValue())
                    .pir(Bandwidth.kBps(redBand.rate()))
                    .pbs(redBand.burst() == null ? null :
                                 redBand.burst().intValue())
                    .yellowAction(yellowAction);
        } else {
            // No yellow action to add; CIR/CBS will be obtained from redBand
            bandwidthProfileBuilder
                    .cir(Bandwidth.kBps(redBand.rate()))
                    .cbs(redBand.burst() == null ? null :
                                 redBand.burst().intValue());
        }

        // Try to add red action in any case
        BandwidthProfileAction redAction =
                getBwProfileActionFromBand(redBand);
        checkNotNull(redAction,
                     "Could not obtain red action from meter band");

        return bandwidthProfileBuilder
                .redAction(redAction)
                .build();
    }

    private static BandwidthProfileAction.Builder getBuilder(Action action) {
        return BandwidthProfileAction.builder().action(action);
    }

    private static BandwidthProfileAction getBwProfileActionFromBand(Band band) {
        checkNotNull(band.type(),
                     "Could not obtain BW profile: Meter band type is null");
        Action action = null;
        if (band.type().equals(Band.Type.DROP)) {
            action = Action.DISCARD;
        } else if (band.type().equals(Band.Type.REMARK)) {
            action = Action.REMARK;
        }
        checkNotNull(action,
                     "Could not obtain BW profile: Invalid meter band type");
        BandwidthProfileAction.Builder actionBuilder = getBuilder(action);
        if (band.type().equals(Band.Type.REMARK)) {
            checkNotNull(band.dropPrecedence(),
                         "Could not obtain DSCP class from meter band");
            actionBuilder.dscpClass(DscpClass.fromShort(band.dropPrecedence()));
        }
        return actionBuilder.build();
    }

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of BandwidthProfile entities.
     */
    public static final class Builder {

        private String name;
        private Bandwidth cir;
        private Bandwidth pir;
        private Integer cbs;
        private Integer pbs;
        private Integer ebs;
        private BandwidthProfileAction greenAction;
        private BandwidthProfileAction yellowAction;
        private BandwidthProfileAction redAction;
        private boolean colorAware;

        /**
         * Sets the name of this bandwidth profile builder.
         *
         * @param name the builder name to set
         * @return this builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the committed information rate (CIR) of this builder.
         *
         * @param cir the builder CIR to set
         * @return this builder instance
         */
        public Builder cir(Bandwidth cir) {
            this.cir = cir;
            return this;
        }

        /**
         * Sets the peak information rate (PIR) of this builder.
         *
         * @param pir the builder PIR to set
         * @return this builder instance
         */
        public Builder pir(Bandwidth pir) {
            this.pir = pir;
            return this;
        }

        /**
         * Sets the committed burst size (CBS) of this builder.
         * The CBS is measured in bytes.
         *
         * @param cbs the builder CBS to set
         * @return this builder instance
         */
        public Builder cbs(Integer cbs) {
            this.cbs = cbs;
            return this;
        }

        /**
         * Sets the peak burst size (PBS) of this builder.
         * The PBS is measured in bytes.
         *
         * @param pbs the builder CBS to set
         * @return this builder instance
         */
        public Builder pbs(Integer pbs) {
            this.pbs = pbs;
            return this;
        }

        /**
         * Sets the excess burst size (EBS) of this builder.
         * The EBS is measured in bytes.
         *
         * @param ebs the builder EBS to set
         * @return this builder instance
         */
        public Builder ebs(Integer ebs) {
            this.ebs = ebs;
            return this;
        }

        /**
         * Sets the green action of this builder.
         *
         * @param greenAction the builder green action to set
         * @return this builder instance
         */
        public Builder greenAction(BandwidthProfileAction greenAction) {
            this.greenAction = greenAction;
            return this;
        }

        /**
         * Sets the yellow action of this builder.
         *
         * @param yellowAction the builder green action to set
         * @return this builder instance
         */
        public Builder yellowAction(BandwidthProfileAction yellowAction) {
            this.yellowAction = yellowAction;
            return this;
        }

        /**
         * Sets the red action of this builder.
         *
         * @param redAction the builder green action to set
         * @return this builder instance
         */
        public Builder redAction(BandwidthProfileAction redAction) {
            this.redAction = redAction;
            return this;
        }

        /**
         * Sets the color-aware mode of this builder.
         *
         * @param colorAware true if profile to be build is color-aware;
         *                   false otherwise
         * @return this builder instance
         */
        public Builder colorAware(boolean colorAware) {
            this.colorAware = colorAware;
            return this;
        }

        /**
         * Builds a new BandwidthProfile instance.
         * based on this builder's parameters
         *
         * @return a new BandwidthProfile instance
         */
        public BandwidthProfile build() {
            checkNotNull(name, "Bandwidth profile must have a name");
            checkNotNull(cir, "Bandwidth profile must have a CIR");
            checkNotNull(greenAction,
                         "Bandwidth profile must have a green action");
            checkNotNull(redAction,
                         "Bandwidth profile must have a red action");
            checkArgument(pir != null || pbs == null,
                          "Bandwidth profile cannot have PBS without PIR");
            checkArgument(pir == null || ebs == null,
                          "Bandwidth profile cannot have both PIR and EBS");
            checkArgument(yellowAction == null && pir == null && ebs == null ||
                                  yellowAction != null &&
                                          (pir != null ^ ebs != null),
                          "Bandwidth profile must have a yellow action only " +
                                  "when either PIR or EBS are defined");
            return new BandwidthProfile(name,
                                        cir, pir, cbs, pbs, ebs,
                                        greenAction, yellowAction, redAction,
                                        colorAware);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(cir, pir, cbs, pbs, ebs,
                            greenAction, yellowAction, redAction,
                            colorAware);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BandwidthProfile) {
            final BandwidthProfile that = (BandwidthProfile) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.cir, that.cir) &&
                    Objects.equals(this.pir, that.pir)  &&
                    Objects.equals(this.cbs, that.cbs) &&
                    Objects.equals(this.pbs, that.pbs)  &&
                    Objects.equals(this.ebs, that.ebs) &&
                    Objects.equals(this.greenAction, that.greenAction)  &&
                    Objects.equals(this.yellowAction, that.yellowAction) &&
                    Objects.equals(this.redAction, that.redAction) &&
                    Objects.equals(this.colorAware, that.colorAware);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("cir", cir)
                .add("pir", pir)
                .add("cbs", cbs)
                .add("pbs", pbs)
                .add("ebs", ebs)
                .add("greenAction", greenAction)
                .add("yellowAction", yellowAction)
                .add("redAction", redAction)
                .add("colorAware", colorAware)
                .toString();
    }
}
