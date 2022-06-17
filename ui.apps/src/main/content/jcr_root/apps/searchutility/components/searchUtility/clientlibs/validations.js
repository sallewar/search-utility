(function(document, $, ns) {
    "use strict";


    $(document).on("click", ".cq-dialog-submit", function(e) {

        var $form = $(this).closest("form.foundation-form"),
            activeAsset = $form.find("[name='./activeAsset']").val(),

            message, clazz = "coral-Button ",
            patterns = {};

	if(activeAsset != null)
    {
       window.location.reload();
    }


    });
})(document, Granite.$, Granite.author);