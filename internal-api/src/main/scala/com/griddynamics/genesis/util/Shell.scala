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
package com.griddynamics.genesis.util

package shell

import math.ScalaNumber
import java.lang.StringBuilder

sealed case class Path(path : String) {
    def / (path : String) = Path(this.path + "/" + path)
    def / (path : Path) = Path(this.path + "/" + path.path)
    override def toString = path
}

object Path {
    implicit def toString(path : Path) = path.path
    implicit def fromString(path : String) = Path(path)
    implicit def fromNull(nil : Null) = DevNull
}

object DevNull extends Path("/dev/null")

sealed trait InputChannel

object StdIn extends InputChannel
case class PathInput(path : Path) extends InputChannel
case class CommandInput(command : Command) extends InputChannel

sealed trait OutputChannel

object StdOut extends OutputChannel
object StdErr extends OutputChannel
case class PathOutput(path : Path, append : Boolean) extends OutputChannel

case class Command(exec : String, // required
                   args : Seq[String] = Seq(),
                   in   : InputChannel = StdIn,
                   out  : OutputChannel = StdOut,
                   err  : OutputChannel = StdErr,
                   fork : Boolean = false) {
    if (exec == null && exec.isEmpty)
        throw new IllegalArgumentException

    import Command._

    def apply(args : Any*) = this.copy(args = this.args ++ args.map(unwrap(_)))

    def & = this.copy(fork = true)

    // TODO think on command.copy(in = CommandInput(this.copy(out = StdOut)))
    def | (command : Command) = command.copy(in = CommandInput(this))

    def > (path : Path) = this.copy(out = PathOutput(path, false))
    def > (nil : Null) = this.copy(out = PathOutput(DevNull, false))
    def > (output : OutputChannel) = this.copy(out = output)

    def >> (path : Path) = this.copy(out = PathOutput(path, true))
    def >> (nil : Null) = this.copy(out = PathOutput(DevNull, true))
    def >> (output : OutputChannel) = this.copy(out = output)

    def ~ (path : Path) = this.copy(err = PathOutput(path, false))
    def ~ (nil : Null) = this.copy(err = PathOutput(DevNull, false))
    def ~ (output : OutputChannel) = this.copy(err = output)

    def ~~ (path : Path) = this.copy(err = PathOutput(path, true))
    def ~~ (nil : Null) = this.copy(err = PathOutput(DevNull, true))
    def ~~ (output : OutputChannel) = this.copy(err = output)

    def &> (path : Path) = this.copy(out = PathOutput(path, false), err = PathOutput(path, false))
    def &> (nil : Null) = this.copy(out = PathOutput(DevNull, false), err = PathOutput(DevNull, false))
    def &> (output : OutputChannel) = this.copy(out = output, err = output)

    def < (path : Path) = this.copy(in = PathInput(path))
    def < (nil : Null) = this.copy(in = PathInput(DevNull))
    def < (input : InputChannel) = this.copy(in = input)

    override def toString = {
 	    val sb = toStringPipe

        fork match {
            case true => sb.insert(0, "(").append(")&").toString
            case false => sb.toString
        }
    }

    private def toStringPipe : StringBuilder = {
        val sb = toStringArgs

        toStringOut(this.out, sb)
        toStringErr(this.err, sb)
        toStringIn(this.in, sb)

        sb
    }

    private def toStringOut(out : OutputChannel, sb : StringBuilder) {
        out match {
            case PathOutput(path, true) => sb.append(" 1>>" + quote(path))
			case PathOutput(path, false) => sb.append(" 1>" + quote(path))
            case StdErr => sb.append(" 1>&2")
            case StdOut => ()
        }
    }

    private def toStringErr(err : OutputChannel, sb : StringBuilder) {
        err match {
            case PathOutput(path, true) => sb.append(" 2>>" + quote(path))
			case PathOutput(path, false) => sb.append(" 2>" + quote(path))
            case StdOut => sb.append(" 2>&1")
            case StdErr => ()
        }
    }

    private def toStringIn(in : InputChannel, sb : StringBuilder) {
        in match {
            case PathInput(path) => sb.append(" <" + quote(path))
            case CommandInput(c) => {
                sb.insert(0, "(")
				sb.insert(1, c.toStringPipe.append(")|("))
                sb.append(")")
            }
            case StdIn => ()
        }
    }

    private def toStringArgs = {
        val sb = new StringBuilder(exec)
        for (a <- args) sb.append(" " + quote(a))
        sb
    }
}

object Command {
    implicit def toString(command : Command) = command.toString

    private def unwrap(arg: Any): String = arg match {
        case n : ScalaNumber => n.underlying.toString
        case _ => arg.asInstanceOf[AnyRef].toString
    }

    def quote(arg : String) : String = {
        arg.contains("'") match {
            case false => "'" + arg + "'"
            case true => "\"" + arg.flatMap(_ match {
                case c if Seq('$', '`', '"', '\\').contains(c) => Seq('\\', c)
                case c if Seq('!', '\n').contains(c) => throw new IllegalArgumentException
                case c => Seq(c)
            }) + "\""
        }
    }
}

package command {
    object exec {
        def apply(exec : String) = Command(exec)
    }

    object nohup {
        def apply(exec : String) = Command("nohup", Seq(exec))
        def apply(command : Command) = command.copy(exec = "nohup", args = command.exec +: command.args)
    }

    object sudo {
        def apply(exec : String) = Command("sudo", Seq(exec))
        def apply(command : Command) = command.copy(exec = "sudo", args = command.exec +: command.args)
    }

    object home extends Path("~")

    object ps extends Command("ps")
    object rm extends Command("rm")
    object grep extends Command("grep")
    object mkdir extends Command("mkdir")
    object chmod extends Command("chmod")
    object echo extends Command("echo")
    object which extends Command("which")
    object `chef-client` extends Command("chef-client")
    object at extends Command("at")
    object yum extends Command("yum")
    object `apt-get` extends Command("apt-get")
}
