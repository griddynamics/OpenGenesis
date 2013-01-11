/*
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

package com.griddynamics.genesis.ldap

import org.scalatest.matchers.ShouldMatchers
import org.junit.Test

class WildcardTest extends ShouldMatchers {

  @Test def testPrefixWildcard() {
    val wildcard = Wildcard("pref*")

    wildcard.accept("pref") should be(true)
    wildcard.accept("prefix") should be(true)
    wildcard.accept("pre") should be(false)
    wildcard.accept("apref") should be(false)
  }

  @Test def testPostfixWildcard() {
    val wildcard = Wildcard("*post")

    wildcard.accept("post") should be(true)
    wildcard.accept("afterpost") should be(true)
    wildcard.accept("ost") should be(false)
    wildcard.accept("postfix") should be(false)
  }

  @Test def testSuffixWildcard() {
    val wildcard = Wildcard("*suff*")

    wildcard.accept("suff") should be(true)
    wildcard.accept("suffix") should be(true)
    wildcard.accept("presuff") should be(true)
    wildcard.accept("prefixsuffix") should be(true)

    wildcard.accept("fix") should be(false)
  }

  @Test def testWildcardIsCaseInsensitive() {
    Wildcard("pref*").accept("Pref") should be (true)
    Wildcard("*post").accept("posT") should be (true)
    Wildcard("*suff*").accept("sUff") should be (true)
  }

  @Test def testAllWildcard() {
    val wildcard = Wildcard("*")

    wildcard.accept("") should be(true)
    wildcard.accept("value") should be(true)
    wildcard.accept("VALUE") should be(true)
  }

  @Test def testWildcardWithSpecialSymbols() {
    Wildcard("John.*").accept("John.Doe") should be (true)
    Wildcard("*\\[username]").accept("[domain]\\[username]") should be (true)
  }

  @Test def testEmptyWildcard() {
    val wildcard = Wildcard("")

    wildcard.accept("") should be(true)
    wildcard.accept("value") should be(false)
    wildcard.accept("VALUE") should be(false)
  }

  @Test def testAcceptNull() {
    Wildcard("*").accept(null) should be (false)
  }

}

class WildcardToWildcardMatchingTest extends ShouldMatchers {

  @Test def testPrefixMatching() {
    val wildcard = Wildcard("pre*")

    wildcard.accept("pre*") should be(true)
    wildcard.accept("Pre*") should be(true)
    wildcard.accept("pref*") should be(true)
    wildcard.accept("preF*") should be(true)
    wildcard.accept("pre") should be(true)
    wildcard.accept("Pre") should be(true)
    wildcard.accept("pref") should be(true)
    wildcard.accept("preF") should be(true)

    wildcard.accept("pr*") should be(false)
    wildcard.accept("*pre") should be(false)
    wildcard.accept("*pre*") should be(false)
    wildcard.accept("*prd*") should be(false)
    wildcard.accept("prd*") should be(false)
    wildcard.accept("*prd") should be(false)
    wildcard.accept("prd") should be(false)
  }

  @Test def testPostfixMatching() {
    val wildcard = Wildcard("*fix")

    wildcard.accept("*fix") should be(true)
    wildcard.accept("*Fix") should be(true)
    wildcard.accept("*postfix") should be(true)
    wildcard.accept("*Postfix") should be(true)
    wildcard.accept("fix") should be(true)
    wildcard.accept("Fix") should be(true)
    wildcard.accept("postfix") should be(true)
    wildcard.accept("Postfix") should be(true)

    wildcard.accept("*ix") should be(false)
    wildcard.accept("fix*") should be(false)
    wildcard.accept("*fix*") should be(false)
    wildcard.accept("*fid*") should be(false)
    wildcard.accept("fid*") should be(false)
    wildcard.accept("*fid") should be(false)
    wildcard.accept("fid") should be(false)
  }

  @Test def testSuffixMatching() {
    val wildcard = Wildcard("*suf*")

    wildcard.accept("*suf*") should be(true)
    wildcard.accept("*sUf*") should be(true)
    wildcard.accept("*asuff*") should be(true)
    wildcard.accept("*AsufF*") should be(true)

    wildcard.accept("suf*") should be (true)
    wildcard.accept("asuf*") should be (true)
    wildcard.accept("sUf*") should be (true)
    wildcard.accept("AsUf*") should be (true)

    wildcard.accept("*suf") should be (true)
    wildcard.accept("*suff") should be (true)
    wildcard.accept("*sUf") should be (true)
    wildcard.accept("*sUff") should be (true)

    wildcard.accept("suf") should be (true)
    wildcard.accept("asuff") should be (true)
    wildcard.accept("AsUff") should be (true)

    wildcard.accept("*uf*") should be (false)
    wildcard.accept("*uf") should be (false)
    wildcard.accept("uf*") should be (false)
    wildcard.accept("uf") should be (false)

    wildcard.accept("*sud*") should be (false)
    wildcard.accept("*sud") should be (false)
    wildcard.accept("sud*") should be (false)
    wildcard.accept("sud") should be (false)

  }
}
