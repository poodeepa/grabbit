package com.twc.webcms.sync.server.services

import com.twc.jackalope.NodeBuilder as FakeNodeBuilder
import com.twc.webcms.sync.server.services.impl.SyncServerServiceImpl
import com.twc.webcms.sync.testutils.MockServletOutputStream
import org.apache.sling.jcr.api.SlingRepository
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static com.twc.jackalope.JCRBuilder.*
import static com.twc.jackalope.JcrConstants.NT_FILE

@Subject(SyncServerService)
class SyncServerServiceSpec extends Specification {

    @Shared
    SlingRepository slingRepository

    @Shared
    ConfigurableApplicationContext configurableApplicationContext

    @Shared
    SyncServerService syncServerService

    def setup() {

        configurableApplicationContext = new ClassPathXmlApplicationContext("META-INF/spring/sync-batch-job.xml")

        FakeNodeBuilder fakeNodeBuilder =
                node("default.groovy",
                        node("jcr:content",
                                property("jcr:data", "foo" )
                        ),
                        property("jcr:primaryType", NT_FILE),
                        property("jcr:lastModified", "Date"),
                        property("multiValueLong", [1L,2L,4L] as Object[]),
                        property("multiValueString", ["a", "b", "c"] as Object[]),
                )


        slingRepository = repository(fakeNodeBuilder).build()

        syncServerService = new SyncServerServiceImpl(slingRepository: slingRepository,
                configurableApplicationContext: configurableApplicationContext)

    }

    def "Service should write data to provided outputStream"() {
        given:
        MockServletOutputStream mockServletOutputStream = new MockServletOutputStream()

        when:
        //This will also actually execute the Batch Job internally
        syncServerService.getContentForRootPath("/default.groovy", mockServletOutputStream)

        then:
        mockServletOutputStream != null
        mockServletOutputStream.toString().contains("default.groovy")
    }

}
