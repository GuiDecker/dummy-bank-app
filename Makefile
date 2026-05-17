.PHONY: build install run logs watch clean uninstall debug

# Variables
PACKAGE := com.guilherme.honeypot
MAIN_ACTIVITY := .ui.MainActivity

# Build debug APK
build:
	./gradlew assembleDebug

# Install on connected device
install:
	./gradlew installDebug

# Build + install + launch app
run: install
	adb shell am start -n $(PACKAGE)/$(MAIN_ACTIVITY)

# App logs
logs:
	adb logcat -s "Honeypot:*" "AndroidRuntime:E" "*:E"

# Continuous rebuild on save
watch:
	./gradlew --continuous assembleDebug

# Clean build cache
clean:
	./gradlew clean

# Uninstall app
uninstall:
	adb uninstall $(PACKAGE)

# Build + install + launch + logs
debug: install
	adb shell am start -n $(PACKAGE)/$(MAIN_ACTIVITY)
	adb logcat -s "Honeypot:*" "AndroidRuntime:E"
