/*
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.plugin.adapter;

import com.griddynamics.genesis.plugin.GenesisStep;
import com.griddynamics.genesis.plugin.StepBuilder;
import com.griddynamics.genesis.plugin.StepBuilder$class;

import java.util.List;
import java.util.Map;

public abstract class AbstractStepBuilder implements StepBuilder {

    private String phase;
    private List<String> precedingPhases;
    private boolean ignoreFail;
    private int retryCount;
    private Map<String, String> exportTo;

    private int id = 0;

    public GenesisStep newStep() {
        return StepBuilder$class.newStep(this);
    }

    /**
     * Simply delegates to scala-generated "implementation" StepBuilder trait.
     */
    public final void $init$() {
        StepBuilder$class.$init$(this);
    }

    /**
     * Scala-style getter.
     *
     * @return - retryCount
     */
    @Override
    public int retryCount() {
        return getRetryCount();
    }

    /**
     * Scala-style setter.
     *
     * @param retryCount -
     */
    @Override
    public void retryCount_$eq(int retryCount) {
        setRetryCount(retryCount);
    }

    /**
     * Scala-style getter.
     *
     * @return precedingPhases
     */
    @Override
    public List<String> precedingPhases() {
        return getPrecedingPhases();
    }

    /**
     * Scala-style setter.
     *
     * @param precedingPhases -
     */
    @Override
    public void precedingPhases_$eq(List<String> precedingPhases) {
        setPrecedingPhases(precedingPhases);
    }

    /**
     * Scala-style getter.
     *
     * @return phase
     */
    @Override
    public String phase() {
        return getPhase();
    }

    /**
     * Scala-style setter.
     *
     * @param phase -
     */
    @Override
    public void phase_$eq(String phase) {
        setPhase(phase);
    }

    /**
     * Scala-style getter.
     *
     * @return exportTo
     */
    @Override
    public Map<String, String> exportTo() {
        return getExportTo();
    }

    /**
     * Scala-style setter.
     *
     * @param exportTo -
     */
    @Override
    public void exportTo_$eq(Map<String, String> exportTo) {
        setExportTo(exportTo);
    }

    /**
     * Scala-style getter.
     *
     * @return ignoreFail
     */
    @Override
    public boolean ignoreFail() {
        return getIgnoreFail();
    }

    /**
     * Scala-style setter.
     *
     * @param ignoreFail -
     */
    @Override
    public void ignoreFail_$eq(boolean ignoreFail) {
        setIgnoreFail(ignoreFail);
    }

    /**
     * Scala-style getter.
     *
     * @return - id
     */
    @Override
    public int id() {
        return getId();
    }

    /**
     * Scala-style setter.
     *
     * @param id -
     */
    @Override
    public void id_$eq(int id) {
        setId(id);
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public List<String> getPrecedingPhases() {
        return precedingPhases;
    }

    public void setPrecedingPhases(List<String> precedingPhases) {
        this.precedingPhases = precedingPhases;
    }

    public boolean getIgnoreFail() {
        return ignoreFail;
    }

    public void setIgnoreFail(boolean ignoreFail) {
        this.ignoreFail = ignoreFail;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Map<String, String> getExportTo() {
        return exportTo;
    }

    public void setExportTo(Map<String, String> exportTo) {
        this.exportTo = exportTo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}