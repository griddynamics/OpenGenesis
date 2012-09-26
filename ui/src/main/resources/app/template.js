require.config({
    baseUrl:'assets/js/libs',
    paths:{
        use:'../plugins/use'
    },
    use:{
        underscore:{
            attach:"_"
        }
    }
});

require([
    'jquery',
    'use!underscore',
    'prettify'
],

function ($, _) {
    $(function () {
        function getURLParameter(name) {
            return decodeURI((new RegExp(name + '=' + '(.+?)(&|$)').exec(document.location.search) || [, null])[1]);
        }

        function getTemplateUrl(projectId, templateName, templateVersion) {
            return "/rest/projects/" + projectId + "/templates/" + templateName + "/v" + templateVersion;
        }

        var projectId = getURLParameter('projectId');
        var template = getURLParameter('templateName');
        var version = getURLParameter('templateVersion');

        var contentEl = $('#content');

        if (projectId === 'null' || template === 'null' || version === 'null') {
            contentEl.html("Not enough parameters.");
            return;
        }

        document.title = template + ' ' + version;

        $.ajax({
            url:getTemplateUrl(projectId, template, version),
            dataType:'json',
            data:{ format:'src' },
            statusCode:{
                401:function () {
                    document.location = '/login.html?expire=true'
                },
                403:function () {
                    contentEl.html('Access denied.')
                },
                404:function () {
                    contentEl.html('Template is not found.')
                }
            }
        }).success(function (response) {
            var content = _.escape(response.content);
            if ($.browser.msie) {
                content = content.replace(/\n/g, '<br/>').replace(/  /g, "&nbsp;");
            }
            contentEl.html(content);
            prettyPrint();
        }).error(function () {
            contentEl.html("Error getting template content.");
        });
    });
});