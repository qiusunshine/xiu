//请求头，结构为{tab: {request: {}}}
var headerMap = {};
browser.webRequest.onSendHeaders.addListener(
    function (data) {
        if (!headerMap[data.tabId]) {
            headerMap[data.tabId] = {};
        }
        headerMap[data.tabId][data.requestId] = data.requestHeaders;
    }, {urls: ["http://*/*", "https://*/*"]}, ["requestHeaders"]);

browser.webRequest.onResponseStarted.addListener(
    function (data) {
        xiu(data);
    }, {urls: ["http://*/*", "https://*/*"]}, ["responseHeaders"]);

//有些请求在onResponseStarted监听不到
browser.webRequest.onBeforeSendHeaders.addListener(
    xiu2,
    {urls: ["*://*/*m3u8*"]},
    ["requestHeaders"]
);
function xiu(data) {
    if (data.tabId == -1) {
        return
    }
    let requestHeaders = (headerMap[data.tabId] || {})[data.requestId] || {};
    let msg = {
        type: "xiu",
        tabId: data.tabId,
        url: data.url,
        method: data.method,
        documentUrl: data.documentUrl,
        requestHeaders: requestHeaders,
        responseHeaders: data.responseHeaders,
        statusCode: data.statusCode
    };
    browser.tabs.get(data.tabId)
    .then(function(tab) {
        msg.documentUrl = tab.url;
        browser.runtime.sendNativeMessage("browser", msg);
    }, function(error) {
        browser.runtime.sendNativeMessage("browser", msg);
    });
}

function xiu2(data) {
    let requestHeaders = data.requestHeaders;
    let msg = {
        type: "xiu",
        tabId: data.tabId,
        url: data.url,
        method: data.method,
        documentUrl: data.documentUrl,
        requestHeaders: requestHeaders,
        responseHeaders: {},
        statusCode: 0
    };
    browser.tabs.get(data.tabId)
    .then(function(tab) {
        msg.documentUrl = tab.url;
        browser.runtime.sendNativeMessage("browser", msg);
    }, function(error) {
        browser.runtime.sendNativeMessage("browser", msg);
    });
    //console.log(data.url);
}

chrome.tabs.onUpdated.addListener(function (tabId, changeInfo) {
    if (changeInfo.status == "loading") {
        if (headerMap[tabId]) {
            delete headerMap[tabId];
        }
    }
});

chrome.tabs.onRemoved.addListener(function (tabId) {
    if (headerMap[tabId]) {
        delete headerMap[tabId];
    }
});
let port = browser.runtime.connectNative("browser");
port.onMessage.addListener(function(message) {
  console.log("Received message from Android:", message);
  if(message.type == "evaluateJavaScript") {
    let code = message.code;
    browser.tabs.query({active: true, currentWindow: true}, function(tabs) {
      var activeTab = tabs[0];
      function onExecuted(result) {
        //console.log(`We executed in all subframes`);
      }

      function onError(error) {
        console.log(`Error: ${error}`);
      }
      let executing = browser.tabs.executeScript(activeTab.id, {code: code, allFrames: true});
      executing.then(onExecuted, onError);
    });
  }
});


