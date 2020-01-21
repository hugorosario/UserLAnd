package tech.ula.utils

import com.nhaarman.mockitokotlin2.* // ktlint-disable no-wildcard-imports
import org.junit.Assert.* // ktlint-disable no-wildcard-imports
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import tech.ula.model.entities.Session
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class ServerUtilityTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @Mock lateinit var mockBusyboxExecutor: BusyboxExecutor

    @Mock lateinit var mockLogUtility: LogUtility

    @Mock lateinit var mockProcess: Process

    lateinit var sshPidFile: File
    lateinit var vncPidFile: File
    lateinit var xsdlPidFile: File

    private val filesystemId = 0L
    private val filesystemDirName = "0"
    private val fakePid = 100L

    lateinit var serverUtility: ServerUtility

    private fun createSshPidFile() {
        val folder = tempFolder.newFolder(filesystemDirName, "run")
        sshPidFile = File("${folder.path}/dropbear.pid")
        sshPidFile.createNewFile()
    }

    private fun createVNCPidFile(session: Session) {
        val folder = tempFolder.newFolder(filesystemDirName, "home", session.username, ".vnc")
        vncPidFile = File("${folder.path}/localhost:${session.port}.pid")
        vncPidFile.createNewFile()
    }

    private fun createXSDLPidFile() {
        val folder = tempFolder.newFolder(filesystemDirName, "tmp")
        xsdlPidFile = File("${folder.absolutePath}/xsdl.pidfile")
        xsdlPidFile.createNewFile()
    }

    @Before
    fun setup() {
        whenever(mockProcess.toString()).thenReturn("pid=$fakePid],")

        serverUtility = ServerUtility(tempFolder.root.path, mockBusyboxExecutor, mockLogUtility)
    }

    @Test
    fun `Calling startServer with an SSH session should use the appropriate command`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "ssh")
        val command = "/support/startSSHServer.sh"

        whenever(mockBusyboxExecutor.executeProotCommand(
                eq(command),
                eq(filesystemDirName),
                eq(false),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()))
                .thenReturn(OngoingExecution(mockProcess))

        createSshPidFile()
        assertTrue(sshPidFile.exists())

        val result = serverUtility.startServer(session)
        assertFalse(sshPidFile.exists())
        assertEquals(fakePid, result)
    }

    @Test
    fun `If starting an ssh server fails, an error is logged and -1 is returned`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "ssh")
        val command = "/support/startSSHServer.sh"

        val reason = "reason"
        whenever(mockBusyboxExecutor.executeProotCommand(
                eq(command),
                eq(filesystemDirName),
                eq(false),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
        ))
                .thenReturn(FailedExecution(reason))

        createSshPidFile()
        assertTrue(sshPidFile.exists())

        val result = serverUtility.startServer(session)

        assertFalse(sshPidFile.exists())
        assertEquals(-1, result)
        verify(mockLogUtility).logRuntimeErrorForCommand("startSSHServer", command, reason)
    }

    @Test
    fun `Calling startServer with a VNC session should use the appropriate command`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "vnc", username = "user", vncPassword = "userland", geometry = "10x10")
        val command = "/support/startVNCServer.sh"
        val env = hashMapOf("INITIAL_USERNAME" to "user", "INITIAL_VNC_PASSWORD" to "userland", "DIMENSIONS" to "10x10")

        whenever(mockBusyboxExecutor.executeProotCommand(
                eq(command),
                eq(filesystemDirName),
                eq(false),
                eq(env),
                anyOrNull(),
                anyOrNull()
        ))
                .thenReturn(OngoingExecution(mockProcess))

        createVNCPidFile(session)
        assertTrue(vncPidFile.exists())

        val result = serverUtility.startServer(session)

        assertFalse(vncPidFile.exists())
        assertEquals(fakePid, result)
    }

    @Test
    fun `If starting a vnc server fails, an error is logged and -1 is returned`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "vnc", username = "user", vncPassword = "userland", geometry = "10x10")
        val command = "/support/startVNCServer.sh"
        val env = hashMapOf("INITIAL_USERNAME" to "user", "INITIAL_VNC_PASSWORD" to "userland", "DIMENSIONS" to "10x10")

        val reason = "reason"
        whenever(mockBusyboxExecutor.executeProotCommand(
                eq(command),
                eq(filesystemDirName),
                eq(false),
                eq(env),
                anyOrNull(),
                anyOrNull()
        ))
                .thenReturn(FailedExecution(reason))

        createVNCPidFile(session)
        assertTrue(vncPidFile.exists())

        val result = serverUtility.startServer(session)

        assertFalse(vncPidFile.exists())
        assertEquals(-1, result)
        verify(mockLogUtility).logRuntimeErrorForCommand("startVNCServer", command, reason)
    }

    @Test
    fun `Calling startServer with an XSDL session should use the appropriate command`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "xsdl", username = "user")
        val command = "/support/startXSDLServer.sh"
        val env = hashMapOf<String, String>()
        env["INITIAL_USERNAME"] = session.username
        env["DISPLAY"] = ":4721"
        env["PULSE_SERVER"] = "127.0.0.1:4721"

        whenever(mockBusyboxExecutor.executeProotCommand(
                eq(command),
                eq(filesystemDirName),
                eq(false),
                eq(env),
                anyOrNull(),
                anyOrNull()
        ))
                .thenReturn(OngoingExecution(mockProcess))

        createXSDLPidFile()
        assertTrue(xsdlPidFile.exists())

        val result = serverUtility.startServer(session)

        assertFalse(xsdlPidFile.exists())
        assertEquals(fakePid, result)
    }

    @Test
    fun `If starting an XSDL server fails, an error is logged and -1 is returned`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "xsdl", username = "user")
        val command = "/support/startXSDLServer.sh"
        val env = hashMapOf<String, String>()
        env["INITIAL_USERNAME"] = session.username
        env["DISPLAY"] = ":4721"
        env["PULSE_SERVER"] = "127.0.0.1:4721"

        val reason = "reason"
        whenever(mockBusyboxExecutor.executeProotCommand(
                eq(command),
                eq(filesystemDirName),
                eq(false),
                eq(env),
                anyOrNull(),
                anyOrNull()
        ))
                .thenReturn(FailedExecution(reason))

        createXSDLPidFile()
        assertTrue(xsdlPidFile.exists())

        val result = serverUtility.startServer(session)

        assertFalse(xsdlPidFile.exists())
        assertEquals(-1, result)
        verify(mockLogUtility).logRuntimeErrorForCommand("setDisplayNumberAndStartTwm", command, reason)
    }

    @Test
    fun `Calling stop service uses the appropriate command`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "ssh")
        serverUtility.stopService(session)
        val command = "support/killProcTree.sh ${session.pid} -1"
        verify(mockBusyboxExecutor).executeCommand(eq(command), anyOrNull())
    }

    @Test
    fun `If stop service fails, an error is logged`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "ssh")
        val command = "support/killProcTree.sh ${session.pid} -1"

        val reason = "reason"
        whenever(mockBusyboxExecutor.executeCommand(eq(command), anyOrNull()))
                .thenReturn(FailedExecution("reason"))

        serverUtility.stopService(session)

        verify(mockLogUtility).logRuntimeErrorForCommand("stopService", command, reason)
    }

    @Test
    fun `Server is always considered running if session type is XSDL`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "xsdl")

        val result = serverUtility.isServerRunning(session)

        assertTrue(result)
        verify(mockBusyboxExecutor, never()).executeCommand(anyOrNull(), anyOrNull())
        verify(mockLogUtility, never()).logRuntimeErrorForCommand(anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `Calls appropriate command to check if server is running, and returns the result`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "ssh")
        val command = "support/isServerInProcTree.sh -1"
        whenever(mockBusyboxExecutor.executeCommand(eq(command), anyOrNull()))
                .thenReturn(SuccessfulExecution)
                .thenReturn(FailedExecution(""))

        val result1 = serverUtility.isServerRunning(session)
        val result2 = serverUtility.isServerRunning(session)

        assertTrue(result1)
        assertFalse(result2)
    }

    @Test
    fun `Logs an error and return false if isServerRunning causes an exception`() {
        val session = Session(0, filesystemId = filesystemId, serviceType = "ssh")
        val command = "support/isServerInProcTree.sh -1"
        val reason = "reason"
        whenever(mockBusyboxExecutor.executeCommand(eq(command), anyOrNull()))
                .thenReturn(FailedExecution(reason))

        val result = serverUtility.isServerRunning(session)

        assertFalse(result)
        verify(mockLogUtility).logRuntimeErrorForCommand("isServerRunning", command, reason)
    }

    @Test
    fun `Calling executeStartCommand on a session without a start command returns early without interecting with busybox`() {
        val session = Session(0, filesystemId = filesystemId)
        val result = serverUtility.executeStartCommand(session)
        assertTrue(result is Unit)
        verifyZeroInteractions(mockBusyboxExecutor)
    }

    @Test(expected = IllegalStateException::class)
    fun `Calling executeStartCommand on a session without a start script throws exception`() {
        val session = Session(0, filesystemId = filesystemId)
        session.startCommand = "ls -lah"

        val folder = tempFolder.newFolder(filesystemDirName, "support")
        val profileScriptFile = File("${folder.absolutePath}/userland_profile.sh")
        profileScriptFile.createNewFile()

        serverUtility.executeStartCommand(session)
    }

    @Test(expected = IllegalStateException::class)
    fun `Calling executeStartCommand on a session without a profile script throws exception`() {
        val session = Session(0, filesystemId = filesystemId)
        session.startCommand = "ls -lah"

        val folder = tempFolder.newFolder(filesystemDirName, "support")
        val startScriptFile = File("${folder.absolutePath}/autostart.sh")
        startScriptFile.createNewFile()

        serverUtility.executeStartCommand(session)
    }

    @Test(expected = IllegalStateException::class)
    fun `Calling executeStartCommand on a session without a start script fails with no interactions with busyboxExecutor`() {
        val session = Session(0, filesystemId = filesystemId)
        session.startCommand = "ls -lah"

        val folder = tempFolder.newFolder(filesystemDirName, "support")
        val profileScriptFile = File("${folder.absolutePath}/userland_profile.sh")
        profileScriptFile.createNewFile()

        try {
            serverUtility.executeStartCommand(session)
        } finally {
            verifyZeroInteractions(mockBusyboxExecutor)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `Calling executeStartCommand on a session without a profile script fails with no interactions with busyboxExecutor`() {
        val session = Session(0, filesystemId = filesystemId)
        session.startCommand = "ls -lah"

        val folder = tempFolder.newFolder(filesystemDirName, "support")
        val startScriptFile = File("${folder.absolutePath}/autostart.sh")
        startScriptFile.createNewFile()

        try {
            serverUtility.executeStartCommand(session)
        } finally {
            verifyZeroInteractions(mockBusyboxExecutor)
        }
    }

    @Test
    fun `Calling executeStartCommand on a session with command and files calls busybox executor`() {
        val session = Session(0, filesystemId = filesystemId)
        session.startCommand = "ls -lah"

        val folder = tempFolder.newFolder(filesystemDirName, "support")
        val startScriptFile = File("${folder.absolutePath}/autostart.sh")
        val profileScriptFile = File("${folder.absolutePath}/userland_profile.sh")
        startScriptFile.createNewFile()
        profileScriptFile.createNewFile()

        serverUtility.executeStartCommand(session)

        val expectedCommand = "/support/autostart.sh"
        val expectedCommandShouldTerminate = false
            verify(mockBusyboxExecutor).executeProotCommand(
                    eq(expectedCommand),
                    eq(filesystemDirName),
                    eq(expectedCommandShouldTerminate),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
            )
    }
}
