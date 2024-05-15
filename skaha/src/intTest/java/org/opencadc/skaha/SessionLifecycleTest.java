/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2020.                            (c) 2020.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 ************************************************************************
 */

package org.opencadc.skaha;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthorizationToken;
import ca.nrc.cadc.net.HttpDelete;
import ca.nrc.cadc.net.HttpPost;
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.skaha.session.Session;
import org.opencadc.skaha.session.SessionAction;

/**
 * @author majorb
 *
 */
public class SessionLifecycleTest {
    
    private static final Logger log = Logger.getLogger(SessionLifecycleTest.class);
    private static final String HOST_PROPERTY = RegistryClient.class.getName() + ".host";
    public static final URI SKAHA_SERVICE_ID = URI.create("ivo://cadc.nrc.ca/skaha");
//    public static final String PROC_SESSION_STDID = "vos://cadc.nrc.ca~vospace/CADC/std/Proc#sessions-1.0";
//    public static final String DESKTOP_IMAGE_SUFFIX = "/skaha/desktop:1.0.2";
//    public static final String CARTA_IMAGE_SUFFIX = "/skaha/carta:3.0";
    public static final String PROD_IMAGE_HOST = "images.canfar.net";
    public static final String DEV_IMAGE_HOST = "images-rc.canfar.net";

    private static final long DEFAULT_TIMEOUT_WAIT_FOR_SESSION_STARTUP_MS = 25 * 1000;

    static {
        Log4jInit.setLevel("org.opencadc.skaha", Level.INFO);
    }
    
    protected final URL sessionURL;
    protected final Subject userSubject;
    protected final String imageHost;

    public SessionLifecycleTest() {
        try {
            // determine image host
            String hostP = System.getProperty(HOST_PROPERTY);
            if (hostP == null || hostP.trim().isEmpty()) {
                throw new IllegalArgumentException("missing server host, check " + HOST_PROPERTY);
            } else {
                hostP = hostP.trim();
                if (hostP.startsWith("rc-")) {
                    imageHost = DEV_IMAGE_HOST;
                } else {
                    imageHost = PROD_IMAGE_HOST;
                }
            }
            
            RegistryClient regClient = new RegistryClient();
            final URL sessionServiceURL =
                    regClient.getServiceURL(SKAHA_SERVICE_ID, Standards.PROC_SESSIONS_10, AuthMethod.TOKEN);
            sessionURL = new URL(sessionServiceURL.toString() + "/session");
            log.info("sessions URL: " + sessionURL);

            final File bearerTokenFile = FileUtil.getFileFromResource("skaha-test.token",
                                                                      ImagesTest.class);
            final String bearerToken = new String(Files.readAllBytes(bearerTokenFile.toPath()));
            userSubject = new Subject();
            userSubject.getPublicCredentials().add(
                    new AuthorizationToken("Bearer", bearerToken.replaceAll("\n", ""),
                                           List.of(NetUtil.getDomainName(sessionURL))));
            log.debug("userSubject: " + userSubject);
        } catch (Exception e) {
            log.error("init exception", e);
            throw new RuntimeException("init exception", e);
        }
    }
    
    @Test
    public void testCreateDeleteSessions() throws Exception {
        Subject.doAs(userSubject, (PrivilegedExceptionAction<Object>) () -> {

            // ensure that there is no active session
            initialize();

            // create desktop session
            createSession("inttest" + SessionAction.SESSION_TYPE_DESKTOP,
                          SessionUtil.getImageOfType(SessionAction.SESSION_TYPE_DESKTOP).getId());

            // until issue 4 (https://github.com/opencadc/skaha/issues/4) has been
            // addressed, just wait for a bit.
            long millisecondCount = 0L;
            final int pollIntervalInSeconds = 5;
            while (getSessions().size() != 1
                   && millisecondCount < SessionLifecycleTest.DEFAULT_TIMEOUT_WAIT_FOR_SESSION_STARTUP_MS) {
                TimeUnit.SECONDS.sleep(pollIntervalInSeconds);
                millisecondCount += pollIntervalInSeconds * 1000;
            }

            // verify desktop session
            verifyOneSession(SessionAction.SESSION_TYPE_DESKTOP, "#1",
                             "inttest" + SessionAction.SESSION_TYPE_DESKTOP);

            // create carta session
            createSession("inttest" + SessionAction.SESSION_TYPE_CARTA,
                          SessionUtil.getImageOfType(SessionAction.SESSION_TYPE_CARTA).getId());

            millisecondCount = 0;
            while (getSessions().size() != 2
                   && millisecondCount < SessionLifecycleTest.DEFAULT_TIMEOUT_WAIT_FOR_SESSION_STARTUP_MS) {
                TimeUnit.SECONDS.sleep(pollIntervalInSeconds);
                millisecondCount += pollIntervalInSeconds * 1000;
            }

            // verify both desktop and carta sessions
            int count = 0;
            List<Session> sessions = getSessions();
            String desktopSessionID = null;
            String cartaSessionID = null;
            String sessionName = null;
            for (Session s : sessions) {
                Assert.assertNotNull("session type", s.getType());
                Assert.assertNotNull("session has no status", s.getStatus());
                if (s.getStatus().equals("Running")) {
                    if (s.getType().equals(SessionAction.SESSION_TYPE_DESKTOP)) {
                        count++;
                        desktopSessionID = s.getId();
                        sessionName = "inttest" + SessionAction.SESSION_TYPE_DESKTOP;
                    } else if (s.getType().equals(SessionAction.SESSION_TYPE_CARTA)) {
                        count++;
                        cartaSessionID = s.getId();
                        sessionName = "inttest" + SessionAction.SESSION_TYPE_CARTA;
                    } else if (!s.getType().equals(SessionAction.TYPE_DESKTOP_APP)) {
                        throw new AssertionError("invalid session type: " + s.getType());
                    }
                    Assert.assertEquals("session name", sessionName, s.getName());
                    Assert.assertNotNull("session id", s.getId());
                    Assert.assertNotNull("connect URL", s.getConnectURL());
                    Assert.assertNotNull("up since", s.getStartTime());
                }
            }
            Assert.assertEquals("should have two sessions", 2, count);
            Assert.assertNotNull("no desktop session", desktopSessionID);
            Assert.assertNotNull("no carta session", cartaSessionID);

            // delete desktop session
            deleteSession(sessionURL, desktopSessionID);

            TimeUnit.SECONDS.sleep(10);

            // verify remaining carta session
            verifyOneSession(SessionAction.SESSION_TYPE_CARTA, "#2",
                             "inttest" + SessionAction.SESSION_TYPE_CARTA);

            // delete carta session
            deleteSession(sessionURL, cartaSessionID);

            TimeUnit.SECONDS.sleep(10);

            // verify that there is no session left
            count = 0;
            sessions = getSessions();
            for (Session s : sessions) {
                Assert.assertNotNull("session ID", s.getId());
                if (s.getId().equals(cartaSessionID) ||
                        s.getId().equals(desktopSessionID)) {
                    count++;
                }
            }
            Assert.assertEquals("zero sessions #2", 0, count);

            return null;
        });
    }
    
    private void initialize() throws Exception {
        List<Session> sessions = getSessions();
        for (Session session : sessions) {
            // skip dekstop-app, deletion of desktop-app is not supported
            if (!session.getType().equals(SessionAction.TYPE_DESKTOP_APP)) {
                deleteSession(sessionURL, session.getId());
            }
        }

        int count = 0;
        sessions = getSessions();
        for (Session s : sessions) {
            if (!s.getType().equals(SessionAction.TYPE_DESKTOP_APP)) {
                count++;
            }
        }
        Assert.assertEquals("zero sessions #1", 0, count);
    }
    private void createSession(final String name, String image) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("image", image);
        params.put("cores", 1);
        params.put("ram", 1);
        HttpPost post = new HttpPost(sessionURL, params, false);
        post.run();
        Assert.assertNull("create session error", post.getThrowable());
    }

    private void verifyOneSession(String expectedSessionType, String sessionNumber, String expectedName)
            throws Exception {
        int count = 0;
        List<Session> sessions = getSessions();
        for (Session session : sessions) {
            Assert.assertNotNull("no session type", session.getType());
            if (session.getType().equals(expectedSessionType)) {
                Assert.assertNotNull("no session ID", session.getId());
                if (session.getStatus().equals("Running"))  {
                    count++;
                    Assert.assertEquals("session name", expectedName, session.getName());
                    Assert.assertNotNull("connect URL", session.getConnectURL());
                    Assert.assertNotNull("up since", session.getStartTime());
                }
            }
            
        }
        Assert.assertEquals("should have one session " + sessionNumber, 1, count);
    }

    private void deleteSession(URL sessionURL, String sessionID) throws MalformedURLException {
        HttpDelete delete = new HttpDelete(new URL(sessionURL.toString() + "/" + sessionID), true);
        delete.run();
        Assert.assertNull("delete session error", delete.getThrowable());
    }
    
    private List<Session> getSessions() throws Exception {
        return SessionUtil.getSessions(sessionURL, Session.STATUS_TERMINATING, Session.STATUS_SUCCEEDED);
    }
}
