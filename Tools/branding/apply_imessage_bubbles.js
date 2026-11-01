// Applies iOS Messages bubble colors to the bundled dark themes:
// iMessage-blue outgoing bubbles with white content, dark gray incoming bubbles.
// Usage: node Tools/branding/apply_imessage_bubbles.js TMessagesProj/src/main/assets/night.attheme ...
const fs = require('fs');

const WHITE = 0xFFFFFFFF, WHITE_70 = 0xB3FFFFFF, WHITE_80 = 0xCCFFFFFF, WHITE_40 = 0x66FFFFFF;
const COLORS = {
  chat_outBubble: 0xFF0A84FF,
  chat_outBubbleSelected: 0xFF0A6FD6,
  chat_inBubble: 0xFF26262A,
  chat_inBubbleSelected: 0xFF333338,
  chat_messageTextOut: WHITE,
  chat_messageLinkOut: WHITE,
  chat_outTimeText: WHITE_70,
  chat_outTimeSelectedText: WHITE_70,
  chat_outSentCheck: WHITE_80,
  chat_outSentCheckSelected: WHITE_80,
  chat_outSentCheckRead: WHITE,
  chat_outSentCheckReadSelected: WHITE,
  chat_outSentClock: WHITE_70,
  chat_outSentClockSelected: WHITE_70,
  chat_outReplyLine: WHITE,
  chat_outReplyNameText: WHITE,
  chat_outReplyMessageText: 0xE6FFFFFF,
  chat_outReplyMediaMessageText: WHITE_70,
  chat_outReplyMediaMessageSelectedText: WHITE_70,
  chat_outForwardedNameText: WHITE,
  chat_outViaBotNameText: WHITE,
  chat_outPreviewLine: WHITE,
  chat_outSiteNameText: WHITE,
  chat_outContactNameText: WHITE,
  chat_outInstant: WHITE,
  chat_outInstantSelected: WHITE,
  chat_outViews: WHITE_70,
  chat_outViewsSelected: WHITE_70,
  chat_outMenu: WHITE_70,
  chat_outMenuSelected: WHITE_70,
  chat_outLoader: WHITE,
  chat_outLoaderSelected: WHITE,
  chat_outFileNameText: WHITE,
  chat_outFileInfoText: WHITE_70,
  chat_outFileInfoSelectedText: WHITE_70,
  chat_outAudioTitleText: WHITE,
  chat_outAudioPerformerText: WHITE_70,
  chat_outAudioPerformerSelectedText: WHITE_70,
  chat_outAudioDurationText: WHITE_70,
  chat_outAudioDurationSelectedText: WHITE_70,
  chat_outAudioSeekbar: WHITE_40,
  chat_outAudioSeekbarFill: WHITE,
  chat_outAudioSeekbarSelected: WHITE_40,
  chat_outAudioCacheSeekbar: WHITE_40,
  chat_outVoiceSeekbar: WHITE_40,
  chat_outVoiceSeekbarFill: WHITE,
  chat_outVoiceSeekbarSelected: WHITE_40,
  chat_outAudioProgress: 0xFF0A84FF,
  chat_outAudioSelectedProgress: 0xFF0A6FD6,
};

const toSigned = (c) => (c | 0);

for (const file of process.argv.slice(2)) {
  const raw = fs.readFileSync(file, 'utf8');
  const eol = raw.includes('\r\n') ? '\r\n' : '\n';
  const lines = raw.split(/\r?\n/);
  const seen = new Set();
  const out = lines.map((line) => {
    const m = /^([A-Za-z0-9_]+)=(-?\d+)$/.exec(line);
    if (m && Object.prototype.hasOwnProperty.call(COLORS, m[1])) {
      seen.add(m[1]);
      return `${m[1]}=${toSigned(COLORS[m[1]])}`;
    }
    return line;
  });
  // insert keys the theme did not define, before any wallpaper section
  const missing = Object.keys(COLORS).filter((k) => !seen.has(k)).map((k) => `${k}=${toSigned(COLORS[k])}`);
  const wpIndex = out.findIndex((l) => l.startsWith('WPS') || l.startsWith('WLS'));
  if (wpIndex >= 0) out.splice(wpIndex, 0, ...missing); else {
    while (out.length && out[out.length - 1] === '') out.pop();
    out.push(...missing, '');
  }
  fs.writeFileSync(file, out.join(eol));
  console.log(`${file}: ${seen.size} updated, ${missing.length} added`);
}
