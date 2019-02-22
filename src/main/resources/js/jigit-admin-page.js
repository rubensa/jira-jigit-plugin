AJS.$(function () {
    AJS.$("#add-jigit-repo-button").click(showAddDialog);
    AJS.$(".edit-jigit-repo-button").click(showEditDialog);
    AJS.$(".add-jigit-repo-branch-button").click(addRepoBranch);
    AJS.$("body").delegate("#change_token", "click", function () {
        var checkbox = AJS.$(this);
        if (checkbox.attr("checked") === 'checked') {
            checkbox.closest("form").find("[name='token']").prop('disabled', false);
        } else {
            checkbox.closest("form").find("[name='token']").prop('disabled', true);
        }
    });
    AJS.$(".remove-jigit-repo-button, .activity-jigit-repo-button, .clear-jigit-repo-button").click(function () {
        getActionTriggerElement(this).closest("tr").find("." + AJS.$(this).attr("data-form")).submit();
    });

    function addRepoBranch() {
        var form = AJS.$(this).closest("tr").find("form");

        AJS.$.ajax({
            url: form.attr("action"),
            type: "POST",
            dataType: "json",
            data: serializeForm(form),
            async: false,
            error: function (xhr) {
                message("#page-message-container",
                    xhr.statusText + (": " + xhr.responseText) || "",
                    AJS.messages.error);
            },
            success: function () {
                form.get(0).reset();
                location.reload();
            }
        });
    }

    function preOpenDialogActions(interior) {
        interior.find("[name='repo_type']").change(function () {
            if (!AJS.$(this).find("option[value='" + this.value + "']").attr("data-single-repo")) {
                interior.find("[name='index_all_branches'], [name='def_branch']").prop("disabled", true);
                interior.find("[name='index_all_branches'][value='true']").prop("checked", true);
            } else {
                interior.find("[name='index_all_branches'], [name='def_branch']").prop("disabled", false);
            }
        });
    }

    function serializeForm(form) {
        var disabled = form.find(':disabled').prop('disabled', false);
        var serializedData = form.serialize();
        disabled.prop('disabled', true);
        return serializedData;
    }

    function showAddDialog() {
        var interior = AJS.$("#add-jigit-repo-container").clone();
        preOpenDialogActions(interior);

        showDialog(interior, "add-jigit-repo-dialog", AJS.I18n.getText("jigit.buttons.add"));
    }

    function getActionTriggerElement(action) {
        return AJS.$("[href='#" + AJS.$(action).closest("div").attr("id") + "']");
    }

    function showEditDialog() {
        var interior = AJS.$("#edit-jigit-repo-container").clone();
        preOpenDialogActions(interior);

        getActionTriggerElement(this).closest("tr").find("[data-val][data-name]").each(function () {
            var element = AJS.$(this);
            var inputs = interior.find("[name='" + element.attr("data-name") + "']");
            inputs.val(inputs.first().is(":radio") ? [element.attr("data-val")] : element.attr("data-val")).change();
        });

        showDialog(interior, "edit-jigit-repo-dialog", AJS.I18n.getText("jigit.buttons.edit"));
    }

    function showDialog(interior, dialogId, headerText) {
        var dialog = new AJS.Dialog({
            width: 600,
            height: 600,
            id: dialogId
        });

        JIRA.bind("Dialog.beforeHide", function (event, dialog, reason) {
            return dialog.attr("id") !== dialogId || reason !== "esc";
        });

        dialog.addHeader(headerText);

        dialog.addPanel("Repo", interior, "");
        interior.show();
        var messageContainerId = "#" + dialogId + " .message-container";

        dialog.addButton(
            AJS.I18n.getText("jigit.buttons.test.connection"),
            function () {
                AJS.$.ajax({
                    url: AJS.contextPath() + "/rest/jigit/1.0/repo/test",
                    type: "POST",
                    dataType: "json",
                    data: serializeForm(interior.find("form")),
                    async: false,
                    error: function (xhr) {
                        message(messageContainerId,
                            xhr.statusText + (": " + xhr.responseText) || "",
                            AJS.messages.error);
                    },
                    success: function () {
                        message(messageContainerId,
                            AJS.I18n.getText("jigit.message.test.connection"),
                            AJS.messages.success);
                    }
                });
            },
            "aui-button"
        );

        dialog.addButton(
            AJS.I18n.getText("jigit.buttons.ok"),
            function () {
                var form = interior.find("form");

                AJS.$.ajax({
                    url: form.attr("action"),
                    type: "POST",
                    dataType: "json",
                    data: serializeForm(form),
                    async: false,
                    error: function (xhr) {
                        message(messageContainerId,
                            xhr.statusText + (": " + xhr.responseText) || "",
                            AJS.messages.error);
                    },
                    success: function () {
                        dialog.remove();
                        form.get(0).reset();
                        location.reload();
                    }
                });

            },
            "aui-button"
        );

        dialog.addCancel(
            AJS.I18n.getText("jigit.buttons.cancel"),
            function (dialog) {
                dialog.remove();
            }
        );

        dialog.show();
    }

    function message(id, text, func) {
        AJS.$(id).empty();
        func.apply(AJS.messages, [id, {
            closeable: true,
            body: text
        }]);
    }
});