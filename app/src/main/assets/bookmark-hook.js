var bookmark = {
    "create": function(e,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "create",
                    event: JSON.stringify(e)
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });

    },
    "get": function(e,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "get",
                    event: JSON.stringify(e)
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });
    },
    "getChildren": function(e,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "getChildren",
                    event: e
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });
    },
    "getRecent": function(number,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "getRecent",
                    event: number + ""
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });
    },
    "getSubTree": function(e,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "getSubTree",
                    event: e
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });
    },
    "getTree": function(callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "getTree"
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });
    },
    "move": function(id,dis,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "move",
                    event:  JSON.stringify({
                        id,
                        changes: dis
                    })
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });
    },
    "onChanged": {
        addListener: function(tab) {

        },
        dispatch: function(tab) {

        },
        hasListener: function(tab) {

        },
        hasListeners: function(tab) {

        },
        removeListener: function(tab) {

        }
    },
    "onCreated": {
        addListener: function(tab) {

        },
        dispatch: function(tab) {

        },
        hasListener: function(tab) {

        },
        hasListeners: function(tab) {

        },
        removeListener: function(tab) {

        }
    },
    "onMoved": {
        addListener: function(tab) {

        },
        dispatch: function(tab) {

        },
        hasListener: function(tab) {

        },
        hasListeners: function(tab) {

        },
        removeListener: function(tab) {

        }
    },
    "onRemoved": {
        addListener: function(tab) {

        },
        dispatch: function(tab) {

        },
        hasListener: function(tab) {

        },
        hasListeners: function(tab) {

        },
        removeListener: function(tab) {

        }
    },
    "remove": function(e,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "remove",
                    event: e
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });
    },
    "removeTree": function(e,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "removeTree",
                    event: e
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });

    },
    "search": function(e,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "search",
                    event: JSON.stringify(e)
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });
    },
    "update": function(id, e,callback) {
        return new Promise((resolve, reject) =>{
            browser.runtime.sendMessage("xiutan@xiu.com", {
                    type: "bookmark-hook",
                    method: "update",
                    event: JSON.stringify({
                        id,
                        changes: e
                    })
                },
                function(e) {
                    resolve(e);
                    callback(e);
                    console.log(e);
                });
        });
    },
}

browser.bookmark = bookmark;
chrome.bookmark = bookmark;
browser.bookmarks = bookmark;
chrome.bookmarks = bookmark;
var bookmarks = bookmark;