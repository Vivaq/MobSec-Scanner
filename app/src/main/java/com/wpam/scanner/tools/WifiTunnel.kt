package com.wpam.scanner.tools

import android.content.Context
import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.apache.sshd.SshServer
import org.apache.sshd.common.ForwardingFilter
import org.apache.sshd.common.SshdSocketAddress
import org.apache.sshd.server.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.random.Random


class WifiTunnel {
    private val sshServerPassword = RandomStringUtils.randomAlphabetic(8)
    private val sshServerPort = Random.nextInt(2000, 10000)

    private lateinit var session: com.jcraft.jsch.Session

    fun startSshServer(sshServerUsername: String): String {
        val sshd = SshServer.setUpDefaultServer()
        sshd.port = sshServerPort
        sshd.keyPairProvider = SimpleGeneratorHostKeyProvider()
        sshd.passwordAuthenticator = PasswordAuthenticator { input_username, input_password, _ ->
            sshServerUsername == input_username && sshServerPassword == input_password
        }
        sshd.tcpipForwardingFilter = object : ForwardingFilter {
            override fun canForwardAgent(session: org.apache.sshd.common.Session): Boolean {
                return true
            }

            override fun canForwardX11(session: org.apache.sshd.common.Session): Boolean {
                return true
            }

            override fun canListen(address: SshdSocketAddress, session: org.apache.sshd.common.Session): Boolean {
                return true
            }

            override fun canConnect(address: SshdSocketAddress, session: org.apache.sshd.common.Session): Boolean {
                return true
            }
        }

        sshd.start()
        return sshServerPassword
    }
    fun setupSShTunnel(connectionString: String, password: String, remoteServerPort: Int, remoteListeningPort: Int): Boolean {
        makeSession(connectionString, password, remoteServerPort)
        createReverseSshTunnel(remoteListeningPort)

        return true
    }

    private fun createReverseSshTunnel(rport: Int) {
        session.setPortForwardingR(rport, "localhost", sshServerPort)
    }

    private fun runSshCommand(command: String): String {
        val sshChannel = session.openChannel("exec") as ChannelExec
        val outputStream = ByteArrayOutputStream()
        sshChannel.outputStream = outputStream

        sshChannel.setCommand(command)
        sshChannel.connect()

        sshChannel.disconnect()

        return outputStream.toString()
    }

    private fun makeSession(connectionString: String, password: String, port: Int): Boolean {
        val jsch = JSch()
        val connectionArr = connectionString.split("@")
        session = jsch.getSession(connectionArr[0], connectionArr[1], port)
        session.setPassword(password)

        val properties = Properties()
        properties["StrictHostKeyChecking"] = "no"
        session.setConfig(properties)
        try {
            session.connect()
        }
        catch (e: JSchException) {
            return false
        }
        return true
    }

    fun stopTunnelling() {
        if (::session.isInitialized)
            session.disconnect()
    }
}