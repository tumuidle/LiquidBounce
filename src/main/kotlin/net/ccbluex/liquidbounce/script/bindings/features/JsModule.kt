/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.script.bindings.features

import com.oracle.truffle.api.`object`.DynamicObject
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.logger

@Suppress("unused")
class JsModule(private val moduleObject: Map<String, Any>) : Module(
    name = moduleObject["name"] as String,
    category = Category.fromReadableName(moduleObject["category"] as String)!!
) {

    private val events = HashMap<String, DynamicObject>()

    private var _tag: String? = null
    override val tag: String?
        get() = _tag

    private var _description: String? = null
    override val description: String
        get() = _description ?: ""

    /**
     * Allows the user to access values by typing module.settings.<valuename>
     */
    val settings by lazy { value }

    init {
        if (moduleObject.containsKey("settings")) {
            val settingsObject = moduleObject["settings"] as Map<String, Value<*>>

            for ((_, value) in settingsObject)
                settings.add(value)
        }

        if (moduleObject.containsKey("tag")) {
            _tag = moduleObject["tag"] as String
        }

        if (moduleObject.containsKey("description")) {
            _description = moduleObject["description"] as String
        }
    }

    /**
     * Called from inside the script to register a new event handler.
     * @param eventName Name of the event.
     * @param handler JavaScript function used to handle the event.
     */
    fun on(eventName: String, handler: Any) {
        println("$eventName $handler " + handler.javaClass.name)

        events[eventName] = handler as DynamicObject
        hookHandler(eventName)
    }

    override fun enable() = callEvent("enable")

    override fun disable() = callEvent("disable")

    /**
     * Calls the function of the [event]  with the [payload] of the event.
     */
    private fun callEvent(event: String, payload: Any? = null) {
        try {
            // events[event]?.call(moduleObject, payload)
        } catch (throwable: Throwable) {
            logger.error("Script caused exception in module $name on $event event!", throwable)
        }
    }

    /**
     * Register new event hook
     */
    private fun hookHandler(eventName: String) {
        val (_, clazz) = EventManager.mappedEvents.find { (name, _) -> name.equals(eventName, true) } ?: return

        EventManager.registerEventHook(
            clazz.java,
            EventHook(
                this,
                {
                    callEvent(eventName, it)
                },
                false
            )
        )
    }

}