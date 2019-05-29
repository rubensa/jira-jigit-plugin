if (JIRA.ViewIssueTabs && JIRA.ViewIssueTabs.onTabReady) {
    JIRA.ViewIssueTabs.onTabReady(function () {
        function copyToClip(text) {
            var input = AJS.$('#jigit-copy-to-clip-container');
            if (input.size() === 0) {
                input = AJS.$("<input type='text' id='jigit-copy-to-clip-container'>");
                AJS.$("body").after(input);
            }
            input.val(text);
            input.show();
            input.select();

            var result = false;
            try {
                result = document.execCommand('copy');
            } catch (err) {
                console.log('copyToClip - unable to copy text to clipboard');
            }
            input.hide();

            return result;
        }

        AJS.$(".jigit-copy-to-clip").off("click").on("click", function () {
            var element = AJS.$(this);
            var sha1 = element.attr("data-sha1");
            if (!sha1) {
                return;
            }
            if (copyToClip(sha1)) {
                element.animate({width: "24px", height: "24px"}, 50);
                element.animate({width: "16px", height: "16px"}, 50);
            }
        });
    })
}
