/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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

!function( $ ){

  "use strict"

  var tabSelector = '[data-tabpanel="header"] > .tab';

  var TabPanel = function (element) {
    $(element).children(".tab").on('click', this.openTab);
  };

  TabPanel.prototype = {

    constructor: TabPanel,

    openTab: function ( e ) {
      var $this = $(this)
        , tabContentSelector = $this.attr('rel')
        , $tabContent;

      $this.siblings('.tab-selected').each(function() {
        var $child = $(this);
        $child.removeClass('tab-selected');
        $($child.attr('rel')).removeClass("opened");
      });

      $this.addClass("tab-selected");

      $(tabContentSelector).addClass("opened");
    }
  };

  $.fn.tabpanel = function ( option ) {
    return this.each(function () {
      var $this = $(this)
        , data = $this.data('tabpanel');
      if (!data) $this.data('tabpanel', (data = new TabPanel(this)));
      if (typeof option == 'string') data[option].call($this)
    })
  };

  $.fn.tabpanel.Constructor = TabPanel;

  $(function () {
    $('body').on('click.tab', tabSelector, TabPanel.prototype.openTab)
  })

}( window.jQuery );