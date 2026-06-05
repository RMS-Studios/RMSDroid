!(() => {
    const { findLazy, Common, onceReady } = RMS.Webpack;
    const ModalEscapeHandler = findLazy(m => m.binds?.length === 1 && m.binds[0] === "esc");

    let isSidebarOpen = false;
    onceReady.then(() => {
        if (Common.FluxDispatcher) {
            Common.FluxDispatcher.subscribe("MOBILE_WEB_SIDEBAR_OPEN", () => {
                isSidebarOpen = true;
            });
            Common.FluxDispatcher.subscribe("MOBILE_WEB_SIDEBAR_CLOSE", () => {
                isSidebarOpen = false;
            });
        }
    });

    window.rmsMobile = {
        // returns true if an action was taken, false if the java side should handle the back press
        onBackPress() {
            try {
                if (ModalEscapeHandler?.action?.() === false) return true;
            } catch (e) {}

            if (!isSidebarOpen && Common.FluxDispatcher) {
                try {
                    Common.FluxDispatcher.dispatch({ type: "MOBILE_WEB_SIDEBAR_OPEN" });
                    return true;
                } catch (e) {}
            }

            return false;
        }
    };

    document.addEventListener("DOMContentLoaded", () => document.documentElement.appendChild(
        Object.assign(document.createElement("link"), {
            rel: "stylesheet",
            type: "text/css",
            href: "https://github.com/zxkuhl/RMS/releases/download/browser/browser.css"
        })
    ), { once: true });
})();
