/**
 *  Predictive Lighting
 *
 *  Copyright 2017 Phil Burch
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Predictive Lighting",
    namespace: "snackwagon",
    author: "Phil Burch",
    description: "Turn on lights based on a trigger sensor. Turn off some of those lights if another sensor doesn't trigger within a specified delay. Turn off the rest of the referenced lights after another specified delay. Light controls for the 'another' sensor are yours to control.",
    category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    //iconUrl: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches.png",
    //iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches@2x.png",
    //iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches@2x.png")


preferences {
	section("Title") {
	}
    section("Turn on when motion detected") {
        input "trigger_motion", "capability.motionSensor", required: true, title: "Select a trigger sensor.."
    }
    /*
    section("These lights will turn on, select them below") {
        input "switches", "capability.switch", required: true, multiple: true, readonly: true
    }
    */
    section("Wait how many seconds for override?") {
    	input "quick_delay", "number", range: "0..500", required: true
    }    
    section("Once motion ends, wait how many seconds?") {
    	input "quiet_delay", "number", range: "0..600", required: true
    }
    section("These lights will always turn off") {
    	input "always_switches", "capability.switch", required: true, multiple: true
    }
    section("And, turn off these lights") {
    	input "override_switches", "capability.switch", required: true, multiple: true
    }
    section("If the following sensor remains inactive") {
        input "override_motion", "capability.motionSensor", required: true, title: "Select an override sensor.."
    }    
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initialized"
    subscribe(trigger_motion, "motion.active", triggerMotionDetectedHandler)
}

// Event handlers

def triggerMotionDetectedHandler(evt) {
    log.debug "triggerMotionDetectedHandler called: $evt"
    state.overrideCancelled = false
    state.currentlyRunning = true
    // do this so primary switches come on first
    always_switches.on()
    override_switches.on()
    subscribe(override_motion, "motion.active", overrideMotionDetectedHandler)
    subscribe(trigger_motion, "motion.inactive", triggerInactiveDetectedHandler)
    runIn(quick_delay, afterQuickDelay)
}

def triggerInactiveDetectedHandler(evt) {
    log.debug "overrideMotionDetectedHandler called: $evt"
    runIn(quiet_delay, afterQuietDelay)
}

def overrideMotionDetectedHandler(evt) {
    log.debug "overrideMotionDetectedHandler called: $evt"
    if(state.currentlyRunning == true) {
    	state.overrideCancelled = true
    }
}

def afterQuickDelay() {
    log.debug "afterQuietDelay called"
    def overrideState = override_motion.currentState("motion")
    if(overrideState.value == "inactive" && state.overrideCancelled == false) {
    	log.debug "Turning off conditional lights"
    	override_switches.off()
    }    
}

def afterQuietDelay() {
    log.debug "afterQuietDelay called"
   
    // if the trigger is seeing motion again, standby
    def triggerState = override_motion.currentState("motion")
    if(triggerState.value == "active") {
    	return
    }

    always_switches.off()
    
    state.overrideCancelled = false
    state.currentlyRunning == false
    try {
    	unsubscribe(override_motion, "motion.active") //, overrideMotionDetectedHandler)
        unsubscribe(trigger_motion, "motion.inactive") //	, triggerInactiveDetectedHandler)
    }
    catch (et) {
    	log.debug et
    }
}