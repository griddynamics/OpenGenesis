/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.executors.json

import com.griddynamics.genesis.workflow.SimpleSyncActionExecutor
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.json.utils.JsonMerge._
import net.liftweb.json.{render, pretty}
import java.io.File
import io.Source
import com.griddynamics.genesis.actions.json.{PreprocessingSuccess, PreprocessingJsonAction}
import net.liftweb.json.JsonAST.JValue

class PreprocessJsonActionExecutor(val action: PreprocessingJsonAction, resourcePath: String) extends SimpleSyncActionExecutor with Logging {
    def startSync() = {
        val name = action.templateName
        val jsonName = resourcePath + "/" + name + ".json"
        val file = new File(jsonName)
        if (file.exists()) {
            val byKey = substituteByKey(Source.fromFile(file), action.keySubst)
            val byPattern = substituteByMask(byKey, action.patternSubst)
            PreprocessingSuccess(action, action.server, pretty(render(merge(byPattern, action.attributes))))
        } else
            PreprocessingSuccess(action, action.server, pretty(render(action.attributes)))
    }
}
