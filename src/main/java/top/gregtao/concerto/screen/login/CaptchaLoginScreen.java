package top.gregtao.concerto.screen.login;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import top.gregtao.concerto.screen.ConcertoScreen;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CaptchaLoginScreen extends ConcertoScreen {
    private TextFieldWidget usernameField, captchaField;
    private ButtonWidget captchaButton;
    private int captchaTimer = -1;
    private final Consumer<String> callForCaptcha;
    private final BiFunction<String, String, Text> loginHandler;
    private final Supplier<Boolean> loginChecker;

    public CaptchaLoginScreen(Consumer<String> callForCaptcha, Supplier<Boolean> loginChecker,
                              BiFunction<String, String, Text> loginHandler, Text title, Screen parent) {
        super(Text.literal(Text.translatable("concerto.screen.login").getString() + title.getString()), parent);
        this.callForCaptcha = callForCaptcha;
        this.loginChecker = loginChecker;
        this.loginHandler = loginHandler;
    }

    @Override
    protected void init() {
        super.init();
        this.usernameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 30, 20, 90, 20, Text.empty());
        this.addSelectableChild(this.usernameField);
        this.addDrawableChild(this.usernameField);
        TextWidget textWidget = new TextWidget(this.width / 2 - 120, 22, 90, 20, Text.translatable("concerto.screen.login.username"), this.textRenderer);
        textWidget.alignLeft();
        this.addDrawableChild(textWidget);
        this.captchaButton = ButtonWidget.builder(Text.translatable("concerto.screen.login.get_captcha"), button -> {
            if (this.usernameField.getText().isEmpty()) {
                this.displayAlert(Text.translatable("concerto.screen.login.empty"));
            } else {
                this.captchaButton.active = false;
                this.captchaTimer = 400;
                this.callForCaptcha.accept(this.usernameField.getText());
            }
        }).position(this.width / 2 + 65, 20).size(60, 20).build();
        this.addDrawableChild(this.captchaButton);

        this.captchaField = new TextFieldWidget(this.textRenderer, this.width / 2 - 30, 50, 155, 20, Text.empty());
        this.addSelectableChild(this.captchaField);
        this.addDrawableChild(this.captchaField);
        TextWidget textWidget1 = new TextWidget(this.width / 2 - 120, 52, 90, 20, Text.translatable("concerto.screen.login.captcha"), this.textRenderer);
        textWidget1.alignLeft();
        this.addDrawableChild(textWidget1);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("concerto.screen.login.confirm"), button -> this.tryLogin())
                .position(this.width / 2 - 32, 80).size(157, 20).build());
    }

    public void tryLogin() {
        String username = this.usernameField.getText().trim(), password = this.captchaField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            this.displayAlert(Text.translatable("concerto.screen.login.empty"));
        } else {
            this.displayAlert(this.loginHandler.apply(username, password));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.loginChecker.get()) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.translatable("concerto.screen.login.success"));
            }
            MinecraftClient.getInstance().setScreen(null);
        }
        if (this.captchaTimer > 0 && --this.captchaTimer == 0) {
            this.captchaButton.active = true;
            this.captchaTimer = -1;
        }
    }
}
