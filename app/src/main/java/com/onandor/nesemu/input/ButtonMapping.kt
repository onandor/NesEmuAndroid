package com.onandor.nesemu.input

import android.view.KeyEvent
import com.onandor.nesemu.R

class ButtonMapping {

    companion object {

        val KEYBOARD_KEYCODE_ICON_MAP = mapOf(
            KeyEvent.KEYCODE_A to R.drawable.ic_keyboard_a,
            KeyEvent.KEYCODE_B to R.drawable.ic_keyboard_b,
            KeyEvent.KEYCODE_C to R.drawable.ic_keyboard_c,
            KeyEvent.KEYCODE_D to R.drawable.ic_keyboard_d,
            KeyEvent.KEYCODE_E to R.drawable.ic_keyboard_e,
            KeyEvent.KEYCODE_F to R.drawable.ic_keyboard_f,
            KeyEvent.KEYCODE_G to R.drawable.ic_keyboard_g,
            KeyEvent.KEYCODE_H to R.drawable.ic_keyboard_h,
            KeyEvent.KEYCODE_I to R.drawable.ic_keyboard_i,
            KeyEvent.KEYCODE_J to R.drawable.ic_keyboard_j,
            KeyEvent.KEYCODE_K to R.drawable.ic_keyboard_k,
            KeyEvent.KEYCODE_L to R.drawable.ic_keyboard_l,
            KeyEvent.KEYCODE_M to R.drawable.ic_keyboard_m,
            KeyEvent.KEYCODE_N to R.drawable.ic_keyboard_n,
            KeyEvent.KEYCODE_O to R.drawable.ic_keyboard_o,
            KeyEvent.KEYCODE_P to R.drawable.ic_keyboard_p,
            KeyEvent.KEYCODE_Q to R.drawable.ic_keyboard_q,
            KeyEvent.KEYCODE_R to R.drawable.ic_keyboard_r,
            KeyEvent.KEYCODE_S to R.drawable.ic_keyboard_s,
            KeyEvent.KEYCODE_T to R.drawable.ic_keyboard_t,
            KeyEvent.KEYCODE_U to R.drawable.ic_keyboard_u,
            KeyEvent.KEYCODE_V to R.drawable.ic_keyboard_v,
            KeyEvent.KEYCODE_W to R.drawable.ic_keyboard_w,
            KeyEvent.KEYCODE_X to R.drawable.ic_keyboard_x,
            KeyEvent.KEYCODE_Y to R.drawable.ic_keyboard_y,
            KeyEvent.KEYCODE_Z to R.drawable.ic_keyboard_z,

            KeyEvent.KEYCODE_0 to R.drawable.ic_keyboard_0,
            KeyEvent.KEYCODE_1 to R.drawable.ic_keyboard_1,
            KeyEvent.KEYCODE_2 to R.drawable.ic_keyboard_2,
            KeyEvent.KEYCODE_3 to R.drawable.ic_keyboard_3,
            KeyEvent.KEYCODE_4 to R.drawable.ic_keyboard_4,
            KeyEvent.KEYCODE_5 to R.drawable.ic_keyboard_5,
            KeyEvent.KEYCODE_6 to R.drawable.ic_keyboard_6,
            KeyEvent.KEYCODE_7 to R.drawable.ic_keyboard_7,
            KeyEvent.KEYCODE_8 to R.drawable.ic_keyboard_8,
            KeyEvent.KEYCODE_9 to R.drawable.ic_keyboard_9,

            KeyEvent.KEYCODE_F1 to R.drawable.ic_keyboard_f1,
            KeyEvent.KEYCODE_F2 to R.drawable.ic_keyboard_f2,
            KeyEvent.KEYCODE_F3 to R.drawable.ic_keyboard_f3,
            KeyEvent.KEYCODE_F4 to R.drawable.ic_keyboard_f4,
            KeyEvent.KEYCODE_F5 to R.drawable.ic_keyboard_f5,
            KeyEvent.KEYCODE_F6 to R.drawable.ic_keyboard_f6,
            KeyEvent.KEYCODE_F7 to R.drawable.ic_keyboard_f7,
            KeyEvent.KEYCODE_F8 to R.drawable.ic_keyboard_f8,
            KeyEvent.KEYCODE_F9 to R.drawable.ic_keyboard_f9,
            KeyEvent.KEYCODE_F10 to R.drawable.ic_keyboard_f10,
            KeyEvent.KEYCODE_F11 to R.drawable.ic_keyboard_f11,
            KeyEvent.KEYCODE_F12 to R.drawable.ic_keyboard_f12,

            KeyEvent.KEYCODE_MINUS to R.drawable.ic_keyboard_minus,                 // -
            KeyEvent.KEYCODE_EQUALS to R.drawable.ic_keyboard_equals,               // =
            KeyEvent.KEYCODE_LEFT_BRACKET to R.drawable.ic_keyboard_bracket_open,   // [
            KeyEvent.KEYCODE_RIGHT_BRACKET to R.drawable.ic_keyboard_bracket_close, // ]
            KeyEvent.KEYCODE_SEMICOLON to R.drawable.ic_keyboard_semicolon,         // ;
            KeyEvent.KEYCODE_APOSTROPHE to R.drawable.ic_keyboard_apostrophe,       // '
            KeyEvent.KEYCODE_COMMA to R.drawable.ic_keyboard_comma,                 // ,
            KeyEvent.KEYCODE_PERIOD to R.drawable.ic_keyboard_period,               // .
            KeyEvent.KEYCODE_SLASH to R.drawable.ic_keyboard_slash_forward,         // /
            KeyEvent.KEYCODE_BACKSLASH to R.drawable.ic_keyboard_slash_back,        // \
            KeyEvent.KEYCODE_GRAVE to R.drawable.ic_keyboard_tilde,                 // `

            KeyEvent.KEYCODE_DPAD_UP to R.drawable.ic_keyboard_arrow_up,
            KeyEvent.KEYCODE_DPAD_DOWN to R.drawable.ic_keyboard_arrow_down,
            KeyEvent.KEYCODE_DPAD_LEFT to R.drawable.ic_keyboard_arrow_left,
            KeyEvent.KEYCODE_DPAD_RIGHT to R.drawable.ic_keyboard_arrow_right,

            KeyEvent.KEYCODE_CTRL_LEFT to R.drawable.ic_keyboard_ctrl,
            KeyEvent.KEYCODE_CTRL_RIGHT to R.drawable.ic_keyboard_ctrl,
            KeyEvent.KEYCODE_SHIFT_LEFT to R.drawable.ic_keyboard_shift,
            KeyEvent.KEYCODE_SHIFT_RIGHT to R.drawable.ic_keyboard_shift,
            KeyEvent.KEYCODE_META_LEFT to R.drawable.ic_keyboard_win,
            KeyEvent.KEYCODE_META_RIGHT to R.drawable.ic_keyboard_win,
            KeyEvent.KEYCODE_ALT_LEFT to R.drawable.ic_keyboard_alt,
            KeyEvent.KEYCODE_ALT_RIGHT to R.drawable.ic_keyboard_alt,
            KeyEvent.KEYCODE_CAPS_LOCK to R.drawable.ic_keyboard_capslock,

            KeyEvent.KEYCODE_DEL to R.drawable.ic_keyboard_backspace,
            KeyEvent.KEYCODE_INSERT to R.drawable.ic_keyboard_insert,
            KeyEvent.KEYCODE_HOME to R.drawable.ic_keyboard_home,
            KeyEvent.KEYCODE_PAGE_UP to R.drawable.ic_keyboard_page_up,
            KeyEvent.KEYCODE_PAGE_DOWN to R.drawable.ic_keyboard_page_down,
            KeyEvent.KEYCODE_FORWARD_DEL to R.drawable.ic_keyboard_delete,
            KeyEvent.KEYCODE_MOVE_END to R.drawable.ic_keyboard_end
        )

        val CONTROLLER_KEYCODE_ICON_MAP = mapOf<Int, Int>(
            KeyEvent.KEYCODE_BUTTON_A to R.drawable.ic_controller_button_a,
            KeyEvent.KEYCODE_BUTTON_B to R.drawable.ic_controller_button_b,
            KeyEvent.KEYCODE_BUTTON_X to R.drawable.ic_controller_button_x,
            KeyEvent.KEYCODE_BUTTON_Y to R.drawable.ic_controller_button_y,
            KeyEvent.KEYCODE_BUTTON_L1 to R.drawable.ic_controller_lb,
            KeyEvent.KEYCODE_BUTTON_L2 to R.drawable.ic_controller_lt,
            KeyEvent.KEYCODE_BUTTON_R1 to R.drawable.ic_controller_rb,
            KeyEvent.KEYCODE_BUTTON_R2 to R.drawable.ic_controller_rt,
            KeyEvent.KEYCODE_BUTTON_START to R.drawable.ic_controller_button_menu,
            KeyEvent.KEYCODE_BUTTON_SELECT to R.drawable.ic_controller_button_view
        )

        val DEFAULT_CONTROLLER_BUTTON_MAP = mapOf<Int, NesButton>(
            KeyEvent.KEYCODE_DPAD_RIGHT to NesButton.DPadRight,
            KeyEvent.KEYCODE_DPAD_LEFT to NesButton.DPadLeft,
            KeyEvent.KEYCODE_DPAD_DOWN to NesButton.DPadDown,
            KeyEvent.KEYCODE_DPAD_UP to NesButton.DPadUp,
            KeyEvent.KEYCODE_BUTTON_START to NesButton.Start,
            KeyEvent.KEYCODE_BUTTON_SELECT to NesButton.Select,
            KeyEvent.KEYCODE_BUTTON_B to NesButton.A,
            KeyEvent.KEYCODE_BUTTON_A to NesButton.B
        )

        val DEFAULT_KEYBOARD_BUTTON_MAP = mapOf<Int, NesButton>(
            KeyEvent.KEYCODE_D to NesButton.DPadRight,
            KeyEvent.KEYCODE_A to NesButton.DPadLeft,
            KeyEvent.KEYCODE_S to NesButton.DPadDown,
            KeyEvent.KEYCODE_W to NesButton.DPadUp,
            KeyEvent.KEYCODE_I to NesButton.Start,
            KeyEvent.KEYCODE_U to NesButton.Select,
            KeyEvent.KEYCODE_K to NesButton.A,
            KeyEvent.KEYCODE_J to NesButton.B
        )
    }
}