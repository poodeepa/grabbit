package com.twc.webcms.sync.server.services.impl

import com.twc.webcms.sync.jcr.JcrUtil
import com.twc.webcms.sync.server.batch.SyncBatchJob
import com.twc.webcms.sync.server.services.NonRecursiveIterator
import com.twc.webcms.sync.server.services.SyncServerService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.jackrabbit.commons.NamespaceHelper
import org.apache.jackrabbit.commons.flat.TreeTraverser
import org.apache.sling.jcr.api.SlingRepository
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.context.ConfigurableApplicationContext

import javax.annotation.Nonnull
import javax.jcr.Node as JcrNode
import javax.jcr.Session
import javax.servlet.ServletOutputStream

@Slf4j
@CompileStatic
@Component(label = "Content Sync Server Service", description = "Content Sync Server Service", immediate = true, metatype = true, enabled = true)
@Service(SyncServerService)
@SuppressWarnings('GroovyUnusedDeclaration')
class SyncServerServiceImpl implements SyncServerService{

    @Reference(bind='setSlingRepository')
    SlingRepository slingRepository

    @Reference(bind='setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext


    void getContentForRootPath(@Nonnull String path, @Nonnull ServletOutputStream servletOutputStream) {

        if(path == null) throw new IllegalStateException("path == null")
        if(servletOutputStream == null) throw new IllegalStateException("servletOutputStream == null")

        JobLauncher jobLauncher = (JobLauncher) configurableApplicationContext.getBean(JobLauncher)

        JcrUtil.withSession(slingRepository, "admin") { Session session ->
            Iterator<JcrNode> nodeIterator

            //If the path is of type "/a/b/.", that means we should not do a recursive search of b's children
            //We should stop after getting all the children of b
            if(path.split("/").last() == "." ) {
                final String actualPath = path.substring(0, path.length() - 2)
                final JcrNode rootNode = session.getNode(actualPath)
                nodeIterator = new NonRecursiveIterator(rootNode)
            }
            else {
                final JcrNode rootNode = session.getNode(path)
                nodeIterator = TreeTraverser.nodeIterator(rootNode)
            }

            SyncBatchJob batchJob = configuredSyncBatchJob(session, nodeIterator, servletOutputStream, path)
            jobLauncher.run(batchJob.getJob(), batchJob.getJobParameters())
        }
    }

    private SyncBatchJob configuredSyncBatchJob(Session session, Iterator<JcrNode> nodeIterator,
                                                ServletOutputStream servletOutputStream, String path) {
        SyncBatchJob batchJob = SyncBatchJob.withApplicationContext(configurableApplicationContext)
                .configure(new NamespaceHelper(session).namespaces.iterator(),
                                nodeIterator, servletOutputStream, path)
                .build()
        batchJob
    }
}