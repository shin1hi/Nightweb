(ns net.nightweb.tabs
  (:use neko.resource
        neko.listeners.view
        neko.find-view))

(defn set-state!
  [this mkey mval]
  (swap! (.state this) assoc-in [mkey] mval))

(defn get-state
  [this mkey]
  (get @(.state this) mkey))

(do
  (gen-class
    :name "net.nightweb.tabs.TabScrollView"
    :extends android.widget.HorizontalScrollView
    :state "state"
    :init "init"
    :post-init "post-init"
    :prefix "tab-scroll-view-"
    :constructors {[android.content.Context] [android.content.Context]
                   [android.content.Context android.util.AttributeSet] [android.content.Context android.util.AttributeSet]
                   [android.content.Context android.util.AttributeSet int] [android.content.Context android.util.AttributeSet int]}
    :methods [["addTab" [] void]
              ["setSelectedTab" [android.view.View] void]
              ["getSelectedTab" [] int]
              ["animateScroll" [int] void]]
    :exposes-methods {onLayout superOnLayout
                      onScrollChanged superOnScrollChanged})
  (gen-class
    :name "net.nightweb.tabs.TabLayout"
    :extends net.nightweb.LinearLayout
    :state "state"
    :init "init"
    :post-init "post-init"
    :prefix "tab-layout-"
    :constructors {[android.content.Context net.nightweb.tabs.TabScrollView] [android.content.Context]}
    :methods []
    :exposes-methods {onMeasure superOnMeasure
                      onLayout superOnLayout})
  (gen-class
    :name "net.nightweb.tabs.TabView"
    :extends net.nightweb.LinearLayout
    :state "state"
    :init "init"
    :post-init "post-init"
    :prefix "tab-view-"
    :constructors {[android.content.Context] [android.content.Context]
                   [android.content.Context android.util.AttributeSet] [android.content.Context android.util.AttributeSet]
                   [android.content.Context android.util.AttributeSet int] [android.content.Context android.util.AttributeSet int]}
    :methods [["setTitle" [String] void]]
    :exposes-methods {onLayout superOnLayout
                      setActivated superSetActivated})
  (gen-class
    :name "net.nightweb.tabs.TabBar"
    :extends net.nightweb.LinearLayout
    :state "state"
    :init "init"
    :post-init "post-init"
    :prefix "tab-bar-"
    :constructors {[android.content.Context] [android.content.Context]
                   [android.content.Context android.util.AttributeSet] [android.content.Context android.util.AttributeSet]
                   [android.content.Context android.util.AttributeSet int] [android.content.Context android.util.AttributeSet int]}
    :methods []
    :exposes-methods {onMeasure superOnMeasure
                      onConfigurationChanged superOnConfigurationChanged}))

(do
  (defn tab-scroll-view-create-state
    [context]
    (atom {:context context
           :selected -1}))
  (defn tab-scroll-view-init
    ([context] [[context] (tab-scroll-view-create-state context)])
    ([context attrs] [[context attrs] (tab-scroll-view-create-state context)])
    ([context attrs def-style] [[context attrs def-style] (tab-scroll-view-create-state context)]))
  (defn tab-scroll-view-post-init
    ([this context]
      (let [res (.getResources context)
            tab-first-padding-left (.getDimension res (get-resource :dimen :tab_first_padding_left))
            content-view (net.nightweb.tabs.TabLayout. context this)
            animation-duration (.getInteger res (get-resource :integer :tab_animation_duration))]
        (.setHorizontalScrollBarEnabled this false)
        (.setOverScrollMode this android.view.View/OVER_SCROLL_NEVER)
        (.setOrientation content-view android.widget.LinearLayout/HORIZONTAL)
        (.setLayoutParams content-view
                          (android.view.ViewGroup$LayoutParams.
                            android.view.ViewGroup$LayoutParams/WRAP_CONTENT
                            android.view.ViewGroup$LayoutParams/MATCH_PARENT))
        (.setPadding content-view tab-first-padding-left 0 0 0)
        (.addView this content-view)
        ;(.scrollTo this (.getScrollX this) (.getScrollY this))
        (set-state! this :content-view content-view)
        (set-state! this :animation-duration animation-duration)))
    ([this context attrs] (tab-scroll-view-post-init this context))
    ([this context attrs def-style] (tab-scroll-view-post-init this context)))
  (defn tab-scroll-view-onLayout
    [this changed l t r b]
    (.superOnLayout this changed l t r b)
    (if-let [child (.getChildAt (get-state this :content-view)
                                (get-state this :selected))]
      (let [childl (.getLeft child)
            childr (+ childl (.getWidth child))
            viewl (.getScrollX this)
            viewr (+ viewl (.getWidth this))]
        (if (< childl viewl)
          (.animateScroll this childl)
          (if (> childr viewr)
            (.animateScroll this (+ (- childr viewr) viewl)))))))
  (defn tab-scroll-view-onScrollChanged
    [this l t oldl oldt]
    (.superOnScrollChanged this l t oldl oldt)
    (let [content-view (get-state this :content-view)]
      (if (.isHardwareAccelerated this)
        (for [i (range 0 (.getChildCount content-view))]
          (.invalidate (.getChildAt content-view i))))))
  (defn tab-scroll-view-addTab
    [this]
    (let [content-view (get-state this :content-view)
          tab-view (net.nightweb.tabs.TabView. (get-state this :context))]
      (.setTitle tab-view "New Tab")
      (.setOnClickListener tab-view (on-click (.setSelectedTab this view)))
      (.addView content-view tab-view)
      (.setActivated tab-view false)))
  (defn tab-scroll-view-setSelectedTab
    [this view]
    (let [content-view (get-state this :content-view)
          num (.indexOfChild content-view view)
          old-view (.getChildAt content-view (get-state this :selected))]
      (if (> num -1)
        (set-state! this :selected num)
        (set-state! this :selected -1))
      (if old-view
        (.setActivated old-view false))
      (if view
        (.setActivated view true))
      (.requestLayout this)))
  (defn tab-scroll-view-getSelectedTab
    [this]
    (get-state this :selected))
  (defn tab-scroll-view-animateScroll
    [this new-scroll]
    (let [animator (android.animation.ObjectAnimator/ofInt
                     this "scroll" (int-array [(.getScrollX this) new-scroll]))]
      (.setDuration animator ^int (get-state this :animation-duration))
      (.start animator)))
  )

(do
  (defn tab-layout-create-state
    [context tabs]
    (let [res (.getResources context)
          tab-overlap (.getDimension res (get-resource :dimen :tab_overlap))]
      (atom {:tab-overlap tab-overlap
             :tabs tabs})))
  (defn tab-layout-init
    ([context tabs] [[context] (tab-layout-create-state context tabs)]))
  (defn tab-layout-post-init
    ([this context]
      (.superSetChildrenDrawingOrderEnabled this true))
    ([this context attrs] (tab-layout-post-init this context))
    ([this context attrs def-style] (tab-layout-post-init this context)))
  (defn tab-layout-onMeasure
    [this hspec vspec]
    (.superOnMeasure this hspec vspec)
    (let [child-count (.getChildCount this)
          max (java.lang.Math/max 0 (- child-count 1))
          tab-overlap (get-state this :tab-overlap)]
      (.superSetMeasuredDimension this
                                  (- (.getMeasuredWidth this) (* max tab-overlap))
                                  (.getMeasuredHeight this))))
  (defn tab-layout-onLayout
    [this changed l t r b]
    (.superOnLayout this changed l t r b)
    (if (> (.getChildCount this) 1)
      (let [tab-overlap (get-state this :tab-overlap)
            first-child (.getChildAt this 0)
            next-left (atom (- (.getRight first-child) tab-overlap))]
        (for [i (range 1 (.getChildCount this))
              :let [tab (.getChildAt this i)
                    w (- (.getRight tab) (.getLeft tab))]]
          (do
            (.layout @next-left (.getTop tab) (+ @next-left w) (.getBottom tab))
            (swap! next-left (fn [old-value] (+ old-value (- w tab-overlap)))))))))
  (defn tab-layout-getChildDrawingOrder
    [this cnt i]
    (let [selected (.getSelectedTab (get-state this :tabs))
          next (- cnt i 1)]
      (if (and (= i (- cnt 1)) (>= selected 0) (< selected cnt))
        selected
        (if (and (<= next selected) (> next 0))
          (- next 1)
          next))))
    )

(do
  (defn tab-view-create-state
    [context]
    (atom {:context context
           :selected false
           :path (android.graphics.Path.)
           :focus-path (android.graphics.Path.)
           :tab-slice-width (get-resource :dimen :tab_slice)
           :tab-overlap (get-resource :dimen :tab_overlap)}))
  (defn tab-view-init
    ([context] [[context] (tab-view-create-state context)])
    ([context attrs] [[context attrs] (tab-view-create-state context)])
    ([context attrs def-style] [[context attrs def-style] (tab-view-create-state context)]))
  (defn tab-view-post-init
    ([this context]
      (.setWillNotDraw this false)
      (.setGravity this android.view.Gravity/CENTER_VERTICAL)
      (.setOrientation this android.widget.LinearLayout/HORIZONTAL)
      (.setPadding this
                   (get-state this :tab-overlap) 0
                   (get-state this :tab-slice-width) 0)
      (let [res (.getResources context)
            tab-width (.getDimension res (get-resource :dimen :tab_width))
            inflater (android.view.LayoutInflater/from context)
            tab-content (.inflate inflater (get-resource :layout :tab_title) this true)
            title (find-view tab-content (get-resource :id :title))
            icon-view (find-view tab-content (get-resource :id :favicon))
            lock (find-view tab-content (get-resource :id :lock))
            close (find-view tab-content (get-resource :id :close))]
        (.setOnClickListener close (on-click (println "close")))
        (set-state! this :tab-width tab-width)
        (set-state! this :tab-content tab-content)
        (set-state! this :title title)
        (set-state! this :icon-view icon-view)
        (set-state! this :lock lock)
        (set-state! this :close close)))
    ([this context attrs] (tab-view-post-init this context))
    ([this context attrs def-style] (tab-view-post-init this context)))
  (defn tab-view-onLayout
    [this changed l t r b]
    (.superOnLayout this changed l t r b)
    (let [path (get-state this :path)
          focus-path (get-state this :focus-path)
          tab-slice-width (get-state this :tab-slice-width)
          local-l 0
          local-t 0
          local-r (- r l)
          local-b (- b t)]
      (.reset path)
      (.moveTo path local-l local-b)
      (.lineTo path local-l local-t)
      (.lineTo path (- local-r tab-slice-width) local-t)
      (.lineTo path local-r local-b)
      (.close path)
      (.reset focus-path)
      (.moveTo focus-path local-l local-b)
      (.lineTo focus-path local-l local-t)
      (.lineTo focus-path (- local-r tab-slice-width) local-t)
      (.lineTo focus-path local-r local-b)))
  (defn tab-view-setActivated
    [this selected]
    (set-state! this :selected selected)
    (let [context (get-state this :context)
          title (get-state this :title)
          icon-view (get-state this :icon-view)
          close (get-state this :close)]
      (.setTextAppearance title context (if selected
                                          (get-resource :style :TabTitleSelected)
                                          (get-resource :style :TabTitleUnselected)))
      (.setVisibility icon-view (if selected android.view.View/GONE android.view.View/VISIBLE))
      (.setVisibility close (if selected android.view.View/VISIBLE android.view.View/GONE))
      (.setHorizontalFadingEdgeEnabled this (not selected))
      (.superSetActivated this selected)
      (if-let [lp (.getLayoutParams this)]
        (do
          (set! (. lp width) (get-state this :tab-width))
          (set! (. lp height) android.view.ViewGroup$LayoutParams/MATCH_PARENT)
          (.setLayoutParams this lp)))
      (.setFocusable this (not selected))
      (.postInvalidate this)))
  (defn tab-view-setTitle
    [this text]
    (let [title (get-state this :title)]
      (.setText title text)))
  )

(do
  (defn tab-bar-create-state
    [context]
    (let [res (.getResources context)]
      (atom {:context context
             :tab-add-overlap (.getDimension res (get-resource :dimen :tab_addoverlap))
             :tab-map {}})))
  (defn tab-bar-init
    ([context] [[context] (tab-bar-create-state context)])
    ([context attrs] [[context attrs] (tab-bar-create-state context)])
    ([context attrs def-style] [[context attrs def-style] (tab-bar-create-state context)]))
  (defn tab-bar-post-init
    ([this context]
      (.inflate (android.view.LayoutInflater/from context)
                (get-resource :layout :tab_bar) this)
      (let [res (.getResources context)
            tab-padding-top (.getDimension res (get-resource :dimen :tab_padding_top))
            tabs (find-view this (get-resource :id :tabs))
            new-tab (find-view this (get-resource :id :newtab))]
        (.setPadding this 0 tab-padding-top 0 0)
        (.setOnClickListener new-tab (on-click (.addTab tabs)))
        (set-state! this :tabs tabs)
        (set-state! this :new-tab new-tab)))
    ([this context attrs] (tab-bar-post-init this context))
    ([this context attrs def-style] (tab-bar-post-init this context)))
  (defn tab-bar-onConfigurationChanged
    [this new-config]
    (.superOnConfigurationChanged this new-config)
    (.updateLayout (get-state this :tabs)))
  (defn tab-bar-onMeasure
    [this hspec vspec]
    (.superOnMeasure this hspec vspec)
    (.superSetMeasuredDimension this
                           (- (.getMeasuredWidth this)
                              (get-state this :tab-add-overlap))
                           (.getMeasuredHeight this)))
  (defn tab-bar-onLayout
    [this changed left top right bottom]
    (let [new-tab (get-state this :new-tab)
          tabs (get-state this :tabs)
          pl (.getPaddingLeft this)
          pt (.getPaddingTop this)
          w (- right left pl)
          tab-add-overlap (get-state this :tab-add-overlap)
          button-width (- (.getMeasuredWidth new-tab) tab-add-overlap)
          tabs-width (.getMeasuredWidth tabs)
          sw (if (< (- w tabs-width) button-width)
               (- w button-width)
               tabs-width)]
      (.layout tabs pl pt (+ pl sw) (- bottom top))
      (.layout new-tab (- (+ pl sw) tab-add-overlap) pt
               (- (+ pl sw button-width) tab-add-overlap) (- bottom top))))
  )

(defn show-tab-bar
  [context]
  (let [action-bar (.getActionBar context)
        tab-bar (net.nightweb.tabs.TabBar. context)]
    (.setNavigationMode action-bar android.app.ActionBar/NAVIGATION_MODE_STANDARD)
    (.setDisplayOptions action-bar android.app.ActionBar/DISPLAY_SHOW_CUSTOM)
    (.setCustomView action-bar tab-bar)))