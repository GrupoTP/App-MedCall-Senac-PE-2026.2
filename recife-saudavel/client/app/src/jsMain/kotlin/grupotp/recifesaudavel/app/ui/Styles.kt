package grupotp.recifesaudavel.app.ui

import ojaynico.kotlin.react.TextStyle
import ojaynico.kotlin.react.ViewStyle

/**
 * React Native port of the prototype stylesheet. Sizes are the CSS rem values times 16.
 * React Native has no relative line height, so each text style states it in pixels.
 */
internal object Styles {
    // Shared

    val fill = ViewStyle(
        position = "absolute",
        top = 0,
        right = 0,
        bottom = 0,
        left = 0,
    )
    val pressed = ViewStyle(transform = arrayOf(scale(0.98)))
    val screen = ViewStyle(flex = 1, backgroundColor = Palette.CANVAS)
    val scroll = ViewStyle(flex = 1)
    val scrollContent = ViewStyle(
        width = "100%",
        maxWidth = CONTENT_MAX_WIDTH,
        alignSelf = "center",
        gap = 16,
        paddingTop = 16,
        paddingHorizontal = 16,
        paddingBottom = 32,
    )
    val card = ViewStyle(
        gap = 16,
        padding = 20,
        borderWidth = 1,
        borderColor = Palette.LINE,
        borderRadius = 16,
        backgroundColor = Palette.WHITE,
    )
    val cardCompact = ViewStyle(
        padding = 16,
        borderWidth = 1,
        borderColor = Palette.LINE,
        borderRadius = 16,
        backgroundColor = Palette.WHITE,
    )

    // Welcome

    val welcome = ViewStyle(flex = 1, backgroundColor = Palette.BLUE_DARK)
    val welcomeScrollContent = ViewStyle(flexGrow = 1)
    val welcomeBody = ViewStyle(
        flex = 1,
        alignItems = "center",
        justifyContent = "center",
        paddingTop = 48,
    )
    val welcomeInner = ViewStyle(
        width = "100%",
        maxWidth = WELCOME_MAX_WIDTH,
        paddingHorizontal = 32,
        paddingBottom = 24,
    )
    val logoBadge = ViewStyle(
        width = 96,
        height = 96,
        alignSelf = "center",
        alignItems = "center",
        justifyContent = "center",
        marginBottom = 24,
        borderRadius = 24,
        borderWidth = 1,
        borderColor = "rgba(255, 255, 255, 0.25)",
        backgroundColor = "rgba(255, 255, 255, 0.15)",
        boxShadow = "0px 20px 25px -5px rgba(0, 0, 0, 0.2)",
    )
    val welcomeTitle = text(
        size = 36,
        lineHeight = 41.4,
        color = Palette.WHITE,
        family = FontFamily.DISPLAY,
        align = "center",
        marginBottom = 8,
    )
    val welcomeTagline = text(size = 16, lineHeight = 24, color = Palette.SOFT, align = "center", marginBottom = 4)
    val welcomeSubtitle = text(size = 14, lineHeight = 21, color = Palette.SOFTER, align = "center", marginBottom = 32)
    val featureList = ViewStyle(gap = 24, marginBottom = 32)
    val feature = ViewStyle(
        flexDirection = "row",
        alignItems = "flex-start",
        gap = 16,
        paddingHorizontal = 4,
    )
    val featureIcon = ViewStyle(marginTop = 2)
    val featureCopy = ViewStyle(flex = 1)
    val featureTitle = text(size = 16, lineHeight = 21.6, color = Palette.WHITE, family = FontFamily.DISPLAY)
    val featureSubtitle = text(size = 14, lineHeight = 18.9, color = Palette.SOFT, marginTop = 2)
    val welcomeActions = ViewStyle(
        width = "100%",
        maxWidth = WELCOME_MAX_WIDTH,
        alignSelf = "center",
        gap = 12,
        paddingHorizontal = 24,
        paddingBottom = 16,
    )
    val welcomePrimaryButton = ViewStyle(
        alignItems = "center",
        padding = 16,
        borderRadius = 16,
        backgroundColor = Palette.WHITE,
        boxShadow = "0px 10px 15px -3px rgba(0, 0, 0, 0.18)",
    )
    val welcomePrimaryButtonText = text(size = 15, lineHeight = 22.5, color = Palette.BLUE)
    val welcomeSecondaryButton = ViewStyle(
        alignItems = "center",
        padding = 16,
        borderRadius = 16,
        borderWidth = 1,
        borderColor = "rgba(255, 255, 255, 0.2)",
        backgroundColor = "rgba(255, 255, 255, 0.1)",
    )
    val welcomeSecondaryButtonText = text(size = 15, lineHeight = 22.5, color = Palette.WHITE)
    val welcomeNote = text(size = 12, lineHeight = 18, color = Palette.SOFTER, align = "center", paddingVertical = 4)
    val welcomeFooter = text(size = 10, lineHeight = 15, color = Palette.FOOTER, align = "center", paddingBottom = 8)

    // Inner screen chrome

    val screenHeader = ViewStyle(
        borderBottomWidth = 1,
        borderBottomColor = Palette.LINE,
        backgroundColor = Palette.WHITE,
    )
    val screenHeaderInner = ViewStyle(
        width = "100%",
        maxWidth = CONTENT_MAX_WIDTH,
        alignSelf = "center",
        flexDirection = "row",
        alignItems = "center",
        gap = 12,
        paddingHorizontal = 16,
        paddingVertical = 14,
    )
    val menuSpace = ViewStyle(width = 40, height = 36)
    val backButton = ViewStyle(
        width = 36,
        height = 36,
        alignItems = "center",
        justifyContent = "center",
        borderRadius = 12,
        backgroundColor = Palette.CANVAS,
    )
    val screenTitle = text(size = 16, lineHeight = 24, family = FontFamily.DISPLAY, flex = 1)
    val appFooter = ViewStyle(
        borderTopWidth = 1,
        borderTopColor = Palette.LINE,
        backgroundColor = Palette.WHITE,
        paddingHorizontal = 16,
        paddingTop = 12,
    )
    val appFooterText = text(size = 10, lineHeight = 15, color = Palette.FOOTER_ON_WHITE, align = "center")

    // Forms

    val sectionTitle = ViewStyle(
        flexDirection = "row",
        alignItems = "center",
        gap = 12,
        marginVertical = 4,
    )
    val sectionRule = ViewStyle(flex = 1, height = 1, backgroundColor = Palette.LINE)
    val sectionTitleText = text(
        size = 11,
        lineHeight = 16.5,
        color = Palette.MUTED,
        family = FontFamily.DISPLAY,
        letterSpacing = 1.1,
        uppercase = true,
    )
    val field = ViewStyle(gap = 4)
    val fieldLabel = text(size = 14, lineHeight = 20)
    val required = text(size = 14, lineHeight = 20, color = Palette.REQUIRED)
    val fieldHint = text(size = 12, lineHeight = 16, color = Palette.MUTED)
    val fieldInput = TextStyle(
        fontFamily = FontFamily.BODY,
        fontSize = 15,
        lineHeight = 22.5,
        color = Palette.INK,
        width = "100%",
        borderWidth = 2,
        borderColor = Palette.LINE,
        borderRadius = 12,
        backgroundColor = Palette.WHITE,
        paddingVertical = 12,
        paddingHorizontal = 16,
    )
    val fieldInputFocused = ViewStyle(
        borderColor = Palette.BLUE,
        boxShadow = "0px 0px 0px 2px rgba(45, 125, 210, 0.2)",
    )
    val fieldInputWithIcon = ViewStyle(paddingRight = 48)
    val passwordField = ViewStyle(justifyContent = "center")
    val passwordToggle = ViewStyle(
        position = "absolute",
        right = 12,
        padding = 4,
    )
    val checkLine = ViewStyle(
        flexDirection = "row",
        alignItems = "center",
        gap = 8,
        marginTop = 4,
    )
    val checkCopy = text(size = 14, lineHeight = 21, color = Palette.MUTED)
    val whatsapp = text(size = 14, lineHeight = 21, color = Palette.WHATSAPP)
    val checkbox = ViewStyle(
        width = 16,
        height = 16,
        alignItems = "center",
        justifyContent = "center",
        borderWidth = 1.5,
        borderColor = Palette.MUTED,
        borderRadius = 3,
        backgroundColor = Palette.WHITE,
    )
    val checkboxChecked = ViewStyle(borderColor = Palette.BLUE, backgroundColor = Palette.BLUE)
    val consentList = ViewStyle(gap = 16)
    val consent = ViewStyle(
        flexDirection = "row",
        alignItems = "flex-start",
        gap = 8,
        padding = 12,
        borderWidth = 2,
        borderColor = Palette.LINE,
        borderRadius = 12,
        backgroundColor = Palette.WHITE,
    )
    val consentSelected = ViewStyle(borderColor = Palette.BLUE, backgroundColor = Palette.CANVAS)
    val consentCheckbox = ViewStyle(marginTop = 3)
    val consentCopy = text(size = 14, lineHeight = 21, flex = 1)
    val alert = ViewStyle(
        flexDirection = "row",
        alignItems = "flex-start",
        gap = 8,
        paddingVertical = 10,
        paddingHorizontal = 12,
        borderWidth = 1,
        borderColor = Palette.ERROR_BORDER,
        borderRadius = 12,
        backgroundColor = Palette.ERROR_BACKGROUND,
    )
    val alertIcon = ViewStyle(marginTop = 2)
    val emoji = text(size = 14, lineHeight = 18)
    val emojiMeta = text(size = 12, lineHeight = 16)
    val alertText = text(size = 14, lineHeight = 21, color = Palette.ERROR_TEXT, flex = 1)
    val formActions = ViewStyle(gap = 12)
    val primaryButton = ViewStyle(
        alignItems = "center",
        paddingVertical = 14,
        paddingHorizontal = 24,
        borderRadius = 12,
        backgroundColor = Palette.BLUE,
        boxShadow = "0px 1px 2px rgba(0, 0, 0, 0.08)",
    )
    val primaryButtonText = text(size = 15, lineHeight = 22.5, color = Palette.WHITE)
    val textLink = ViewStyle(alignSelf = "flex-start")
    val textLinkCenter = ViewStyle(alignItems = "center", paddingVertical = 8)
    val textLinkText = text(size = 14, lineHeight = 21, color = Palette.BLUE)
    val textLinkTextCenter = text(size = 14, lineHeight = 21, color = Palette.BLUE, align = "center")
    val requiredNote = text(size = 12, lineHeight = 16, color = Palette.MUTED)

    // Floating menu

    val menuLayer = ViewStyle(
        position = "absolute",
        top = 0,
        right = 0,
        bottom = 0,
        left = 0,
        zIndex = 50,
    )
    val menuBackdrop = ViewStyle(
        position = "absolute",
        top = 0,
        right = 0,
        bottom = 0,
        left = 0,
        backgroundColor = "rgba(0, 0, 0, 0.2)",
    )
    val menuButton = ViewStyle(
        position = "absolute",
        left = 16,
        width = 40,
        height = 40,
        alignItems = "center",
        justifyContent = "center",
        borderRadius = 16,
        borderWidth = 1,
        borderColor = Palette.LINE,
        backgroundColor = Palette.WHITE,
        boxShadow = "0px 10px 15px -3px rgba(0, 0, 0, 0.12)",
    )
    val menuButtonOpen = ViewStyle(borderColor = Palette.INK, backgroundColor = Palette.INK)
    val menuPanel = ViewStyle(
        position = "absolute",
        left = 16,
        width = 256,
        overflow = "hidden",
        borderWidth = 1,
        borderColor = Palette.LINE,
        borderRadius = 16,
        backgroundColor = Palette.WHITE,
        boxShadow = "0px 8px 40px rgba(45, 125, 210, 0.18)",
    )
    val menuGreeting = ViewStyle(
        paddingHorizontal = 16,
        paddingVertical = 12,
        backgroundColor = Palette.BLUE,
    )
    val menuGreetingHello = text(size = 12, lineHeight = 18, color = Palette.GREETING)
    val menuGreetingName = text(size = 14, lineHeight = 21, color = Palette.WHITE)
    val menuBody = ViewStyle(paddingVertical = 8)
    val menuLabel = text(
        size = 10,
        lineHeight = 15,
        color = Palette.MUTED,
        family = FontFamily.DISPLAY,
        uppercase = true,
        letterSpacing = 1.5,
    )
    val menuLabelBox = ViewStyle(paddingHorizontal = 16, paddingTop = 8, paddingBottom = 4)
    val menuDivider = ViewStyle(height = 1, marginHorizontal = 12, marginVertical = 4, backgroundColor = Palette.CANVAS)
    val menuItem = ViewStyle(
        flexDirection = "row",
        alignItems = "center",
        gap = 12,
        paddingHorizontal = 16,
        paddingVertical = 10,
    )
    val menuItemDangerHover = ViewStyle(backgroundColor = Palette.ERROR_BACKGROUND)
    val menuItemActive = ViewStyle(backgroundColor = Palette.CANVAS)
    val menuItemText = text(size = 14, lineHeight = 21, color = Palette.INK, flex = 1)
    val menuItemTextActive = text(size = 14, lineHeight = 21, color = Palette.BLUE, flex = 1)
    val menuItemTextDanger = text(size = 14, lineHeight = 21, color = Palette.REQUIRED, flex = 1)
    val menuActiveDot = ViewStyle(width = 6, height = 6, borderRadius = 3, backgroundColor = Palette.BLUE)

    // Home

    val homeHeader = ViewStyle(paddingTop = 16, paddingBottom = 24, backgroundColor = Palette.BLUE_HOVER)
    val homeHeaderInner = ViewStyle(
        width = "100%",
        maxWidth = CONTENT_MAX_WIDTH,
        alignSelf = "center",
        paddingHorizontal = 16,
    )
    val homeHeaderTools = ViewStyle(flexDirection = "row", justifyContent = "flex-end", marginBottom = 16)
    val homeIconButton = ViewStyle(
        width = 36,
        height = 36,
        alignItems = "center",
        justifyContent = "center",
        borderRadius = 12,
        backgroundColor = "rgba(255, 255, 255, 0.15)",
    )
    val homeGreeting = text(size = 14, lineHeight = 21, color = Palette.GREETING)
    val homeName = text(size = 22, lineHeight = 27.5, color = Palette.WHITE, family = FontFamily.DISPLAY, marginTop = 2)
    val homeEmail = text(size = 12, lineHeight = 18, color = Palette.SOFTER, marginTop = 4)
    val disclaimer = ViewStyle(
        flexDirection = "row",
        alignItems = "flex-start",
        gap = 8,
        paddingVertical = 10,
        paddingHorizontal = 12,
        borderWidth = 1,
        borderColor = Palette.WARNING_BORDER,
        borderRadius = 12,
        backgroundColor = Palette.WARNING_BACKGROUND,
    )
    val disclaimerText = text(size = 12, lineHeight = 18, color = Palette.WARNING_TEXT, flex = 1)
    val eyebrow = text(
        size = 11,
        lineHeight = 16.5,
        color = Palette.MUTED,
        family = FontFamily.DISPLAY,
        letterSpacing = 1.1,
        uppercase = true,
        marginBottom = 12,
    )
    val eyebrowInline = text(
        size = 11,
        lineHeight = 16.5,
        color = Palette.MUTED,
        family = FontFamily.DISPLAY,
        letterSpacing = 1.1,
        uppercase = true,
    )
    val sectionRow = ViewStyle(
        flexDirection = "row",
        alignItems = "center",
        justifyContent = "space-between",
        marginBottom = 12,
    )
    val quickGrid = ViewStyle(gap = 12)
    val quickRow = ViewStyle(flexDirection = "row", gap = 12)
    val quickTile = ViewStyle(
        flex = 1,
        gap = 12,
        padding = 16,
        borderWidth = 2,
        borderColor = Palette.LINE,
        borderRadius = 12,
        backgroundColor = Palette.CANVAS,
    )
    val quickTileGreen = ViewStyle(borderColor = Palette.GREEN_BORDER, backgroundColor = Palette.GREEN_BACKGROUND)
    val quickWide = ViewStyle(
        flexDirection = "row",
        alignItems = "center",
        gap = 12,
        padding = 16,
        borderWidth = 2,
        borderColor = Palette.PURPLE_BORDER,
        borderRadius = 12,
        backgroundColor = Palette.PURPLE_BACKGROUND,
    )
    val quickIcon = ViewStyle(
        width = 40,
        height = 40,
        alignItems = "center",
        justifyContent = "center",
        borderRadius = 12,
        backgroundColor = Palette.BLUE_ICON_BACKGROUND,
    )
    val quickIconGreen = ViewStyle(backgroundColor = Palette.GREEN_ICON_BACKGROUND)
    val quickIconPurple = ViewStyle(backgroundColor = Palette.PURPLE_ICON_BACKGROUND)
    val quickCopy = ViewStyle(flex = 1)
    val quickTitle = text(size = 14, lineHeight = 20, family = FontFamily.DISPLAY)
    val quickSubtitle = text(size = 12, lineHeight = 16, color = Palette.MUTED, marginTop = 2)
    val quickTileDisabled = ViewStyle(opacity = 0.55)
    val upcomingList = ViewStyle(gap = 8)
    val upcomingRow = ViewStyle(
        flexDirection = "row",
        alignItems = "center",
        gap = 12,
        padding = 12,
        borderRadius = 12,
        backgroundColor = Palette.CANVAS,
    )
    val dateBadge = ViewStyle(
        width = 44,
        height = 44,
        alignItems = "center",
        justifyContent = "center",
        borderRadius = 12,
        backgroundColor = Palette.BLUE_ICON_BACKGROUND,
    )
    val dateBadgeDay = text(size = 11, lineHeight = 14, color = Palette.BLUE, family = FontFamily.DISPLAY)
    val dateBadgeMonth = text(size = 10, lineHeight = 13, color = Palette.MUTED)
    val upcomingCopy = ViewStyle(flex = 1, minWidth = 0)
    val upcomingTitle = text(size = 14, lineHeight = 20)
    val upcomingSubtitle = text(size = 12, lineHeight = 16, color = Palette.MUTED)

    // Alerts and buttons of the scheduling screens

    val infoAlert = ViewStyle(
        flexDirection = "row",
        alignItems = "flex-start",
        gap = 8,
        paddingVertical = 10,
        paddingHorizontal = 12,
        borderWidth = 1,
        borderColor = Palette.GREEN_BORDER,
        borderRadius = 12,
        backgroundColor = Palette.GREEN_BACKGROUND,
    )
    val infoAlertText = text(size = 12, lineHeight = 18, color = Palette.GREEN_TEXT, flex = 1)
    val infoAlertStrong = text(size = 12, lineHeight = 18, color = Palette.GREEN_TEXT, bold = true)
    val warnAlert = ViewStyle(
        flexDirection = "row",
        alignItems = "flex-start",
        gap = 8,
        paddingVertical = 10,
        paddingHorizontal = 12,
        borderWidth = 1,
        borderColor = Palette.WARNING_BORDER,
        borderRadius = 12,
        backgroundColor = Palette.WARNING_BACKGROUND,
    )
    val warnAlertText = text(size = 12, lineHeight = 18, color = Palette.WARNING_TEXT, flex = 1)
    val buttonRow = ViewStyle(flexDirection = "row", gap = 12)
    val buttonGrow = ViewStyle(flex = 1)
    val buttonDisabled = ViewStyle(opacity = 0.5)
    val secondaryButton = ViewStyle(
        alignItems = "center",
        paddingVertical = 12,
        paddingHorizontal = 20,
        borderWidth = 2,
        borderColor = Palette.LINE,
        borderRadius = 12,
        backgroundColor = Palette.CANVAS,
    )
    val secondaryButtonText = text(size = 15, lineHeight = 22.5, color = Palette.BLUE)
    val fieldInputMultiline = TextStyle(minHeight = 88, textAlignVertical = "top")
    val fieldError = text(size = 12, lineHeight = 16, color = Palette.ERROR_TEXT)
    val loadingText = text(size = 14, lineHeight = 21, color = Palette.MUTED, align = "center", paddingVertical = 16)

    // Step indicator

    val steps = ViewStyle(
        borderBottomWidth = 1,
        borderBottomColor = Palette.LINE,
        backgroundColor = Palette.WHITE,
    )
    val stepsInner = ViewStyle(
        width = "100%",
        maxWidth = CONTENT_MAX_WIDTH,
        alignSelf = "center",
        paddingHorizontal = 16,
        paddingVertical = 12,
    )
    val stepsTop = ViewStyle(
        flexDirection = "row",
        alignItems = "center",
        justifyContent = "space-between",
        marginBottom = 6,
    )
    val stepsCounter = text(size = 12, lineHeight = 16, color = Palette.BLUE, family = FontFamily.DISPLAY)
    val stepsRemaining = ViewStyle(
        paddingHorizontal = 8,
        paddingVertical = 2,
        borderWidth = 1,
        borderColor = Palette.LINE,
        borderRadius = 999,
        backgroundColor = Palette.CANVAS,
    )
    val stepsRemainingLast = ViewStyle(borderColor = Palette.SUCCESS_BORDER, backgroundColor = Palette.SUCCESS_BACKGROUND)
    val stepsRemainingText = text(size = 11, lineHeight = 14, color = Palette.STEP_TEXT, family = FontFamily.DISPLAY)
    val stepsRemainingTextLast = text(size = 11, lineHeight = 14, color = Palette.SUCCESS_TEXT, family = FontFamily.DISPLAY)
    val stepsLabel = text(size = 15, lineHeight = 20, family = FontFamily.DISPLAY, marginBottom = 10)
    val stepsBars = ViewStyle(flexDirection = "row", alignItems = "flex-end", gap = 6)
    val stepsColumn = ViewStyle(flex = 1, alignItems = "center", gap = 2)
    val stepsNumber = text(size = 10, lineHeight = 10, color = Palette.BLUE, bold = true)
    val stepsNumberDone = ViewStyle(opacity = 0.7)
    val stepsNumberPending = ViewStyle(opacity = 0)
    val stepsBar = ViewStyle(width = "100%", height = 8, borderRadius = 999, backgroundColor = Palette.LINE)
    val stepsBarDone = ViewStyle(backgroundColor = Palette.BLUE)
    val stepsBarActive = ViewStyle(height = 12, backgroundColor = Palette.BLUE)
    val stepsNote = text(size = 11, lineHeight = 14, color = Palette.MUTED, marginTop = 6)

    // Scheduling choices

    val chipWrap = ViewStyle(flexDirection = "row", flexWrap = "wrap", gap = 8)
    val chip = ViewStyle(
        paddingVertical = 10,
        paddingHorizontal = 14,
        borderWidth = 2,
        borderColor = Palette.LINE,
        borderRadius = 12,
        backgroundColor = Palette.WHITE,
    )
    val chipSelected = ViewStyle(borderColor = Palette.BLUE, backgroundColor = Palette.BLUE)
    val chipText = text(size = 14, lineHeight = 20)
    val chipTextSelected = text(size = 14, lineHeight = 20, color = Palette.WHITE)
    // Four per row, like the prototype grid. A fixed width keeps the last row from stretching.
    val timeChip = ViewStyle(
        width = "22%",
        alignItems = "center",
        paddingVertical = 10,
        borderWidth = 2,
        borderColor = Palette.LINE,
        borderRadius = 12,
        backgroundColor = Palette.WHITE,
    )
    val summaryList = ViewStyle(gap = 8, marginTop = 8)
    val summaryListLoose = ViewStyle(gap = 12)
    val summaryRow = ViewStyle(
        flexDirection = "row",
        justifyContent = "space-between",
        alignItems = "center",
        gap = 12,
        paddingVertical = 6,
        borderBottomWidth = 1,
        borderBottomColor = Palette.CANVAS,
    )
    val summaryRowLoose = ViewStyle(paddingVertical = 4)
    val summaryRowLast = ViewStyle(borderBottomWidth = 0)
    val summaryLabel = text(size = 14, lineHeight = 20, color = Palette.MUTED)
    val summaryValue = text(size = 14, lineHeight = 20, align = "right", flex = 1)

    // Success

    val successScroll = ViewStyle(flexGrow = 1, justifyContent = "center")
    val successInner = ViewStyle(
        width = "100%",
        maxWidth = 512,
        alignSelf = "center",
        gap = 16,
        paddingHorizontal = 20,
        paddingVertical = 32,
    )
    val successIcon = ViewStyle(
        width = 80,
        height = 80,
        alignSelf = "center",
        alignItems = "center",
        justifyContent = "center",
        borderRadius = 24,
        backgroundColor = Palette.BLUE_ICON_BACKGROUND,
    )
    val successTitle = text(size = 24, lineHeight = 32, family = FontFamily.DISPLAY, align = "center")
    val successSubtitle = text(size = 14, lineHeight = 21, color = Palette.MUTED, align = "center", marginTop = 8)
    val successIconGreen = ViewStyle(backgroundColor = Palette.SUCCESS_BACKGROUND)
    val successHeading = ViewStyle(marginVertical = 4)

    // Appointments list

    val appointmentCard = ViewStyle(
        gap = 8,
        padding = 16,
        borderWidth = 1,
        borderColor = Palette.LINE,
        borderRadius = 16,
        backgroundColor = Palette.WHITE,
    )
    val appointmentCancelled = ViewStyle(opacity = 0.6)
    val appointmentTop = ViewStyle(flexDirection = "row", alignItems = "flex-start", gap = 8)
    val appointmentTitle = text(size = 14, lineHeight = 20, family = FontFamily.DISPLAY)
    val appointmentMeta = ViewStyle(flexDirection = "row", alignItems = "center", flexWrap = "wrap", gap = 16)
    val appointmentMetaItem = ViewStyle(flexDirection = "row", alignItems = "center", gap = 4)
    val appointmentMetaText = text(size = 12, lineHeight = 16, color = Palette.MUTED)
    val cancelButton = ViewStyle(flexDirection = "row", alignItems = "center", alignSelf = "flex-start", gap = 6, paddingVertical = 2)
    val cancelButtonText = text(size = 12, lineHeight = 16, color = Palette.CANCELLED_TEXT, bold = true)
    val statusPill = ViewStyle(paddingHorizontal = 10, paddingVertical = 4, borderRadius = 999)
    val statusPending = ViewStyle(backgroundColor = Palette.PENDING_BACKGROUND)
    val statusConfirmed = ViewStyle(backgroundColor = Palette.SUCCESS_BACKGROUND)
    val statusCancelled = ViewStyle(backgroundColor = Palette.CANCELLED_BACKGROUND)
    val statusPendingText = text(size = 12, lineHeight = 16, color = Palette.PENDING_TEXT)
    val statusConfirmedText = text(size = 12, lineHeight = 16, color = Palette.CONFIRMED_TEXT)
    val statusCancelledText = text(size = 12, lineHeight = 16, color = Palette.CANCELLED_TEXT)
    val emptyState = ViewStyle(alignItems = "center", gap = 12, paddingVertical = 48)
    val emptyStateText = text(size = 14, lineHeight = 21, color = Palette.MUTED, align = "center")

    // Settings

    val tabBar = ViewStyle(
        borderBottomWidth = 1,
        borderBottomColor = Palette.LINE,
        backgroundColor = Palette.WHITE,
    )
    val tabBarInner = ViewStyle(
        width = "100%",
        maxWidth = CONTENT_MAX_WIDTH,
        alignSelf = "center",
        flexDirection = "row",
        gap = 4,
        paddingHorizontal = 16,
        paddingVertical = 12,
    )
    val tab = ViewStyle(flex = 1, alignItems = "center", paddingVertical = 8, borderRadius = 12)
    val tabSelected = ViewStyle(backgroundColor = Palette.BLUE)
    val tabText = text(size = 12, lineHeight = 16, color = Palette.MUTED)
    val tabTextSelected = text(size = 12, lineHeight = 16, color = Palette.WHITE)
    val cardTight = ViewStyle(
        gap = 8,
        padding = 16,
        borderWidth = 1,
        borderColor = Palette.LINE,
        borderRadius = 16,
        backgroundColor = Palette.WHITE,
    )
    val dangerRow = ViewStyle(
        flexDirection = "row",
        alignItems = "center",
        gap = 12,
        paddingVertical = 12,
        paddingHorizontal = 12,
        borderRadius = 12,
    )
    val dangerText = text(size = 14, lineHeight = 20, color = Palette.CANCELLED_TEXT)
    val noteText = text(size = 12, lineHeight = 18, color = Palette.MUTED, paddingVertical = 0)
    val consentRow = ViewStyle(
        flexDirection = "row",
        alignItems = "flex-start",
        gap = 12,
        padding = 12,
        borderWidth = 1,
        borderColor = Palette.LINE,
        borderRadius = 12,
        backgroundColor = Palette.CANVAS,
    )
    val consentRowCopy = ViewStyle(flex = 1)
    val consentRowTitle = text(size = 14, lineHeight = 20)
    val consentRowText = text(size = 12, lineHeight = 16, color = Palette.MUTED, marginTop = 2)
    val linkCard = ViewStyle(
        flexDirection = "row",
        alignItems = "center",
        gap = 12,
        padding = 16,
        borderWidth = 1,
        borderColor = Palette.LINE,
        borderRadius = 16,
        backgroundColor = Palette.WHITE,
    )
    val policyHero = ViewStyle(gap = 4, padding = 16, borderRadius = 16, backgroundColor = Palette.BLUE)
    val policyHeroTitle = text(size = 14, lineHeight = 20, color = Palette.WHITE, family = FontFamily.DISPLAY)
    val policyHeroText = text(size = 12, lineHeight = 18, color = "rgba(255, 255, 255, 0.8)")
    val policyTitle = text(size = 14, lineHeight = 20, family = FontFamily.DISPLAY)
    val policyText = text(size = 14, lineHeight = 22, color = Palette.MUTED, marginTop = 6)

    // Confirmation dialog

    val dialogBackdrop = ViewStyle(
        flex = 1,
        alignItems = "center",
        justifyContent = "center",
        padding = 16,
        backgroundColor = "rgba(0, 0, 0, 0.4)",
    )
    val dialogCard = ViewStyle(
        width = "100%",
        maxWidth = 400,
        gap = 16,
        padding = 20,
        borderRadius = 16,
        backgroundColor = Palette.WHITE,
        boxShadow = "0px 10px 15px -3px rgba(0, 0, 0, 0.12)",
    )
    val dialogTitle = text(size = 16, lineHeight = 24, family = FontFamily.DISPLAY)
    val dialogText = text(size = 14, lineHeight = 21, color = Palette.MUTED)
}

private fun text(
    size: Number,
    lineHeight: Number,
    color: String = Palette.INK,
    family: String = FontFamily.BODY,
    align: String? = undefined,
    letterSpacing: Number? = undefined,
    uppercase: Boolean = false,
    flex: Number? = undefined,
    marginTop: Number? = undefined,
    marginBottom: Number? = undefined,
    paddingVertical: Number? = undefined,
    paddingBottom: Number? = undefined,
    bold: Boolean = false,
) = TextStyle(
    fontFamily = family,
    fontSize = size,
    lineHeight = lineHeight,
    color = color,
    textAlign = align,
    letterSpacing = letterSpacing,
    fontWeight = if (bold) "700" else undefined,
    textTransform = if (uppercase) "uppercase" else undefined,
    flex = flex,
    marginTop = marginTop,
    marginBottom = marginBottom,
    paddingVertical = paddingVertical,
    paddingBottom = paddingBottom,
)

private fun scale(value: Double): dynamic {
    val transform: dynamic = js("({})")
    transform.scale = value
    return transform
}
