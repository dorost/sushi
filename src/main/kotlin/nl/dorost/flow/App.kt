package nl.dorost.flow

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.default
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import nl.dorost.flow.core.Action
import nl.dorost.flow.core.Branch
import nl.dorost.flow.core.FlowEngine
import nl.dorost.flow.utils.ResponseMessage
import java.io.File
import java.lang.Exception
import java.util.*

fun main(args: Array<String>) {
    val flowEngine = FlowEngine()
    val objectMapper = ObjectMapper()

    val server = embeddedServer(Netty, port = 8080) {
        routing {


            get("/") {
                call.respondFile(File(Thread.currentThread().contextClassLoader.getResource("static/index.html").path))
            }

            static("/") {
                resources("static")
            }

            post("/tomlToDigraph") {
                val tomlString = call.receiveText()
                val digraph: String?
                try {
                    flowEngine.flows.clear()
                    val blocks = flowEngine.tomlToBlocks(tomlString)
                    flowEngine.wire(blocks)
                    digraph = flowEngine.blocksToDigraph(blocks)
                    call.respond(
                        HttpStatusCode.OK,
                        objectMapper.writeValueAsString(
                            ResponseMessage(
                                responseLog = "Successfully converted TOML to Digraph.",
                                digraphData = digraph
                            )
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        objectMapper.writeValueAsString(
                            ResponseMessage(
                                responseLog = e.message
                                    ?: "Something went wrong in the server side! ${e.localizedMessage}"
                            )
                        )
                    )
                }

            }


            get("/getAction/{actionId}") { pipelineContext ->
                val actionId = call.parameters["actionId"]
                val action = flowEngine.flows.firstOrNull { it.id == actionId }

                action?.let {
                    call.respond(
                        HttpStatusCode.OK,
                        objectMapper.writeValueAsString(
                            ResponseMessage(
                                blockInfo = it,
                                responseLog = "Successfully fetched action info for ${it.id}"
                            )
                        )
                    )
                } ?: run {
                    call.respond(
                        HttpStatusCode.NotFound,
                        objectMapper.writeValueAsString(
                            ResponseMessage(
                                responseLog = "Couldn't find action with id: ${actionId}"
                            )
                        )
                    )
                }

            }


            get("/addAction/{actionType}") {
                val actionType = call.parameters["actionType"]
                val registeredBlock = flowEngine.registeredActions.firstOrNull { it.type == actionType }

                registeredBlock?.let { action ->
                    flowEngine.flows.add(
                        action.apply {
                            id = UUID.randomUUID().toString()
                        }
                    )
                    flowEngine.wire(flowEngine.flows)
                    val toml = flowEngine.blocksToToml(flowEngine.flows)
                    val digraph = flowEngine.blocksToDigraph(flowEngine.flows)
                    call.respond(
                        HttpStatusCode.OK,
                        objectMapper.writeValueAsString(
                            ResponseMessage(
                                responseLog = "Successfully added new action to flow!",
                                tomlData = toml,
                                digraphData = digraph
                            )
                        )
                    )
                }


            }

            get("/getLibrary") {
                call.respond(
                    HttpStatusCode.OK,
                    objectMapper.writeValueAsString(
                        ResponseMessage(
                            responseLog = "Fetched library of registered actions!",
                            library = flowEngine.registeredActions
                        )
                    )
                )
            }

            get("/deleteBlock/{blockId}") { pipelineContext ->
                val actionId = call.parameters["blockId"]

                flowEngine.flows.removeIf { it.id == actionId }
                flowEngine.flows.forEach { block ->
                    when (block) {
                        is Action -> {
                            block.nextBlocks.removeIf { it == actionId }
                        }
                        is Branch -> {
                            block.mapping.entries.removeIf { it.value == actionId }
                        }
                    }
                }
                // Rebuild graph and send back the response

                val digraph = flowEngine.blocksToDigraph(flowEngine.flows)
                val tomlText: String = flowEngine.blocksToToml(flowEngine.flows)
                call.respond(
                    HttpStatusCode.OK,
                    objectMapper.writeValueAsString(
                        ResponseMessage(
                            responseLog = "Action Removed successfully!",
                            digraphData = digraph,
                            tomlData = tomlText
                        )
                    )
                )


            }
        }
    }
    server.start(wait = true)
}