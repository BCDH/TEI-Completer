/*
 * Example of a JSON Response transformation written
 * in JavaScript
 *
 * The `content` param is a JSON object in the format
 * described in: https://github.com/BCDH/TEI-Completer#server-messages
 */
function transform(content) {
    var suggestion = [];

    for(var i = 0; i < content.sgns.length; i++) {
        suggestion.push(
            {
                "tc:value" : content.sgns[i].v,
                "tc:description" : content.sgns[i].d
            }
        )
    }

    var suggestions = {
        "tc:suggestion": suggestion
    }

    return suggestions;
}