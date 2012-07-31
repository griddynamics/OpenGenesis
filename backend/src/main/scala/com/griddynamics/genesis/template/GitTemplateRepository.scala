/**
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
package com.griddynamics.genesis.template

import java.io.File
import scala.collection.{JavaConversions=>JC}
import scala.collection.mutable
import com.griddynamics.genesis.service.Credentials
import org.eclipse.jgit.api._
import org.eclipse.jgit.transport._
import org.eclipse.jgit.treewalk.TreeWalk
import org.apache.commons.io.FilenameUtils
import org.eclipse.jgit.treewalk.filter.TreeFilter
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}
import org.eclipse.jgit.lib.{ObjectId, Constants}
import com.griddynamics.genesis.util.Logging

class GitTemplateRepository(uri : String,
                            credentials : Credentials,
                            branch : String,
                            directory : File,
                            wildcard : String,
                            charset : String) extends ModeAwareTemplateRepository with Logging {
    import GitTemplateRepository._

    lazy val repo = initRepository
    lazy val git = new Git(repo)

    var lastRev = ""
    var sources = Map[VersionedTemplate, String]()

    val treeFilter = new WildcardTreeFilter(wildcard)

    def respondTo = Modes.Git

    def initRepository = {
        log.info("Starting git-init for repository '%s' in directory '%s'", uri, directory.getAbsolutePath)

        val initCommand = Git.init

        initCommand.setBare(true)
        initCommand.setDirectory(directory)

        val repo = initCommand.call().getRepository()

        val config = new RemoteConfig(repo.getConfig, genesisRemoteName)

        for (uri <- JC.asScalaBuffer(config.getURIs).toList)
            config.removeURI(uri)

        for (refSpec <- JC.asScalaBuffer(config.getFetchRefSpecs).toList)
            config.removeFetchRefSpec(refSpec)

        config.addURI(new URIish(uri))

        var refSpec = new RefSpec()
        refSpec = refSpec.setForceUpdate(true)
        refSpec = refSpec.setSourceDestination(Constants.R_HEADS + "*",
                                               Constants.R_HEADS + "*");
        config.addFetchRefSpec(refSpec)

        config.update(repo.getConfig())
		repo.getConfig().save()

        log.info("Operation git-init for repository '%s' was completed", uri)

        repo
    }

    def fetchRepository() {
        log.info("Starting git-fetch for repository '%s' in directory '%s'", uri, directory.getAbsolutePath)

        val fetchCommand = git.fetch()

        fetchCommand.setRemote(genesisRemoteName)

        fetchCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
            credentials.identity, credentials.credential
        ))

        fetchCommand.setTagOpt(TagOpt.NO_TAGS)

        fetchCommand.call()

        log.info("Operation git-fetch for repository '%s' was completed", uri)
    }

    def listSources() = {
        fetchRepository()

        val revWalk = new RevWalk(repo)
        val head = repo.getRef(Constants.R_HEADS + branch)

        val commit = revWalk.parseCommit(head.getObjectId)

        if (lastRev != commit.getId.getName) {
            val treeWalk = createTreeWalk(commit)
            val rev = commit.getId.getName
            val fetchedSources = mutable.Map[VersionedTemplate, String]()

            while (treeWalk.next()) {
                val id = treeWalk.getObjectId(0)
                val key = VersionedTemplate(treeWalk.getNameString, id.getName)
                fetchedSources(key) = sources.getOrElse(key, getBlob(id))
            }

            sources = fetchedSources.toMap
            lastRev = rev
        }

        sources
    }

    def createTreeWalk(commit : RevCommit) = {
        val tree = commit.getTree
        val treeWalk = new TreeWalk(repo)

        treeWalk.setFilter(treeFilter)
        treeWalk.setRecursive(true)
        treeWalk.addTree(tree)

        treeWalk
    }

    def getBlob(id : ObjectId) = {
        new String(repo.open(id).getBytes, charset)
    }
}

object GitTemplateRepository {
    val genesisRemoteName = Constants.DEFAULT_REMOTE_NAME
}

class WildcardTreeFilter(wildcard : String) extends TreeFilter {
    def shouldBeRecursive() = false

    def include(walker: TreeWalk) = {
        walker.isSubtree || FilenameUtils.wildcardMatch(walker.getPathString, wildcard,
                                                        TemplateRepository.wildCardIOCase)
    }

    override def clone() = {
        new WildcardTreeFilter(wildcard)
    }
}
