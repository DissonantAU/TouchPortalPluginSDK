package com.christophecvb.touchportal.samplekotlin.invokable.connector

import com.christophecvb.touchportal.TPConnector
import com.christophecvb.touchportal.TouchPortalPlugin
import com.christophecvb.touchportal.annotations.Connector
import com.christophecvb.touchportal.annotations.ConnectorValue
import com.christophecvb.touchportal.annotations.Data
import com.christophecvb.touchportal.model.TPListChangedMessage
import com.christophecvb.touchportal.samplekotlin.TouchPortalSampleKotlinPlugin

import java.util.logging.Logger

@Connector(
    name = "Example Class Connector",
    categoryId = "CategoryWithSubs",
    format = "Connect Example Class Connector with param {\$param\$}",
    subCategoryId = "Cat1"
)
class ExampleClassConnector(touchPortalPlugin: TouchPortalSampleKotlinPlugin?) :
    TPConnector<TouchPortalSampleKotlinPlugin?>(touchPortalPlugin) {

    companion object {
        private val LOGGER: Logger = Logger.getLogger(TouchPortalPlugin::class.java.name)
    }

    @ConnectorValue
    private val value: Int? = null

    @Data
    private val param: String? = null

    override fun onInvoke() {
        LOGGER.info("Example Class Connector with param=" + this.param + " and value=" + this.value)
    }

    override fun onListChanged(tpListChangedMessage: TPListChangedMessage) {
        LOGGER.info("ExampleClassConnector.onListChanged")
    }
}
