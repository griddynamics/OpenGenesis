package com.griddynamics.genesis.rest

import org.springframework.web.bind.annotation._
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.RemoteAgentsService
import javax.validation.Valid
import com.griddynamics.genesis.api.RemoteAgent
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.api.RemoteAgent
import com.griddynamics.genesis.api.RemoteAgent

@Controller
@RequestMapping(value = Array("/rest/agents"))
class RemoteAgentController extends RestApiExceptionsHandler {
    @Autowired
    var service: RemoteAgentsService = _

    @RequestMapping(method = Array(RequestMethod.GET))
    @ResponseBody
    def list() = service.list

    @ResponseBody @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
    def get(@PathVariable(value = "id") key: Int) = service.get(key).getOrElse(throw new ResourceNotFoundException("Couldn't find agent"))


    @ResponseBody @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.PUT))
    def update(@PathVariable(value = "id") key: Int, @Valid @RequestBody agent: RemoteAgent) = {
        val existing: RemoteAgent = get(key)
        service.update(agent)
    }

    @ResponseBody @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
    def delete(@PathVariable(value = "id") key: Int) = {
        val agent: RemoteAgent = get(key)
        service.delete(agent)
    }

    @ResponseBody @RequestMapping(method = Array(RequestMethod.POST))
    def create(@Valid @RequestBody agent: RemoteAgent) = {
        service.create(agent)
    }
}
