#!/bin/bash
#
# Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
#   http://www.griddynamics.com
#
#   This library is free software; you can redistribute it and/or modify it under the terms of
#   the GNU Lesser General Public License as published by the Free Software Foundation; either
#   version 2.1 of the License, or any later version.
#
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
#   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
#   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
#   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
#   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
#   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
#   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
#   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
#   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
#   Project:     Genesis
#   Description:  Continuous Delivery Platform
#


# temporary fix for unavailable yum repo
rm -f /etc/yum.repos.d/elff.repo
rm -f /etc/yum.repos.d/elff-testing.repo

if [ `which chef-client &>/dev/null; echo $?` -ne 0 ]; then
    if [ `which apt-get &>/dev/null; echo $?` -eq 0 ]; then
        apt-get -y --force-yes update
        apt-get -y --force-yes install ruby ruby-dev libopenssl-ruby rdoc ri irb build-essential wget ssl-cert
    elif [ `which yum &>/dev/null; echo $?` -eq 0 ]; then
        rpm -Uvh http://download.fedora.redhat.com/pub/epel/5/i386/epel-release-5-4.noarch.rpm
        rpm -Uvh http://download.elff.bravenet.com/5/i386/elff-release-5-3.noarch.rpm
        yum -y update
        yum -y install ruby ruby-shadow ruby-ri ruby-rdoc gcc gcc-c++ ruby-devel ruby-static make wget ntpdate
    else
        exit 1
    fi

    cd /tmp
    wget http://production.cf.rubygems.org/rubygems/rubygems-1.3.7.tgz
    tar zxf rubygems-1.3.7.tgz
    cd rubygems-1.3.7
    ruby setup.rb --no-format-executable

    gem install --no-rdoc --no-ri chef

fi
ntpdate 0.pool.ntp.org

exit `which chef-client &>/dev/null; echo $?`
