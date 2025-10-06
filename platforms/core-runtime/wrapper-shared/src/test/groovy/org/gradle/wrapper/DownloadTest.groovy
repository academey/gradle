/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.wrapper

import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.test.fixtures.server.http.BlockingHttpServer
import org.junit.Rule
import spock.lang.Specification

class DownloadTest extends Specification {

    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider(getClass());

    @Rule
    BlockingHttpServer server = new BlockingHttpServer()

    def "downloads file"() {
        given:
        def destination = tmpDir.file('destinationDir/file')
        def remoteFile = tmpDir.file('remoteFile') << 'sometext'
        def sourceUrl = remoteFile.toURI()

        when:
        def download = new Download(new Logger(true), "gradlew", "aVersion")
        download.download(sourceUrl, destination)

        then:
        destination.exists()
        destination.text == 'sometext'
    }

    def "fails with clear error message on HTTP 404"() {
        given:
        server.start()
        def destination = tmpDir.file('destinationDir/file')
        def downloadUrl = new URI("${server.uri}/gradle-dist.zip")

        and:
        server.expect(server.get("/gradle-dist.zip").sendError(404, "Not Found"))

        when:
        def download = new Download(new Logger(true), "gradlew", "aVersion")
        download.download(downloadUrl, destination)

        then:
        def ex = thrown(IOException)
        ex.message.contains("Download failed with HTTP status code: 404")
        !destination.exists()
    }

    def "fails with clear error message on HTTP 302 redirect"() {
        given:
        server.start()
        def destination = tmpDir.file('destinationDir/file')
        def downloadUrl = new URI("${server.uri}/gradle-dist.zip")

        and:
        server.expect(server.get("/gradle-dist.zip").sendError(302, "Found"))

        when:
        def download = new Download(new Logger(true), "gradlew", "aVersion")
        download.download(downloadUrl, destination)

        then:
        def ex = thrown(IOException)
        ex.message.contains("Download failed with HTTP status code: 302")
        !destination.exists()
    }

    def "fails with clear error message on HTTP 500 server error"() {
        given:
        server.start()
        def destination = tmpDir.file('destinationDir/file')
        def downloadUrl = new URI("${server.uri}/gradle-dist.zip")

        and:
        server.expect(server.get("/gradle-dist.zip").sendError(500, "Internal Server Error"))

        when:
        def download = new Download(new Logger(true), "gradlew", "aVersion")
        download.download(downloadUrl, destination)

        then:
        def ex = thrown(IOException)
        ex.message.contains("Download failed with HTTP status code: 500")
        !destination.exists()
    }

}
