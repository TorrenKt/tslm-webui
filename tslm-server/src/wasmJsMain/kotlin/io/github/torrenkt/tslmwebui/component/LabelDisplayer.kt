package io.github.torrenkt.tslmwebui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.torrenkt.tslm.TslmEntity
import io.github.torrenkt.tslm.TslmLabel
import io.github.torrenkt.tslmwebui.*
import io.github.torrenkt.tslmwebui.core.codePointCount
import io.github.torrenkt.tslmwebui.core.logger
import io.github.torrenkt.tslmwebui.routers.TslmDisplayEntity
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val log by logger("LabelDisplayer")

@Composable
fun LabelDisplayer(
    input: String,
    labels: List<TslmDisplayEntity>,
    fontSize: TextUnit = 22.sp,
    modifier: Modifier = Modifier,
) {
    val validatedLabels = remember(input, labels) {
        val sorted = labels
            .filter { it.start >= 0 && it.end <= input.codePointCount && it.start < it.end }
            .sortedBy { it.start }

        val displayLabels = mutableListOf<TslmDisplayEntity>()
        var pos = 0

        for (entity in sorted) {
            if (entity.start > pos) {
                displayLabels += TslmDisplayEntity(pos, entity.start, null)
            }
            displayLabels += entity
            pos = entity.end
        }

        if (pos < input.codePointCount) {
            displayLabels += TslmDisplayEntity(pos, input.codePointCount, null)
        }

        displayLabels.toList()
    }

    Box(modifier) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ){
            items(
                count = validatedLabels.size,
                key = { validatedLabels[it] },
            ) { index ->
                val label = validatedLabels[index]
                LabelDisplayerItem(
                    item = label(input),
                    type = label.label,
                    fontSize = fontSize,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelDisplayerItem(
    item: String,
    type: TslmLabel?,
    fontSize: TextUnit = 22.sp,
) {
    if (type == null) {
        Text(
            text = item,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 5.dp)
                .wrapContentSize(),
            textAlign = TextAlign.Center,
            fontSize = fontSize,
            color = Color(0xFF000000)
        )
        return
    }

    val state = rememberTooltipState()
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(
                    text = stringResource(type.name()),
                )
            }
        },
        state = state,
    ) {
        Card(
            modifier = Modifier.padding(horizontal = 2.dp)
                .defaultMinSize(minWidth = 22.dp)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = type.color,
            ),
        ) {
            Text(
                text = item,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .padding(horizontal = 3.dp, vertical = 5.dp)
                    .wrapContentSize(),
                textAlign = TextAlign.Center,
                fontSize = fontSize,
                color = Color(0xFF000000)
            )
        }
    }
}

val TslmLabel.color: Color get() {
    return when (this) {
        // Title 系列（浅红 / 粉系）
        TslmLabel.Title -> Color(0xFFFFCDD2)     // pink100

        // 基础信息（浅橙黄）
        TslmLabel.Season -> Color(0xFFFFE0B2)         // orange lighten-4
        TslmLabel.Episode -> Color(0xFFFFCC80)
        TslmLabel.Producer -> Color(0xFFFFD54F)
        TslmLabel.Year -> Color(0xFFFFEB3B)           // yellow500
        TslmLabel.Month -> Color(0xFFFFF176)
        TslmLabel.Type -> Color(0xFFFFE082)
        TslmLabel.Release_State -> Color(0xFFFFB74D)
        TslmLabel.Release_Version -> Color(0xFFFFA726)

        // 字幕相关（浅绿）
        TslmLabel.Subtitle_Language -> Color(0xFFC8E6C9) // green100
        TslmLabel.Subtitle_Format -> Color(0xFFA5D6A7)
        TslmLabel.Subtitle_Type -> Color(0xFF81C784)

        // 文件基础属性（浅蓝）
        TslmLabel.File_Type -> Color(0xFFBBDEFB)         // blue100
        TslmLabel.File_Source -> Color(0xFF90CAF9)
        TslmLabel.File_Platform -> Color(0xFF64B5F6)

        // 视频相关（青蓝系）
        TslmLabel.File_Video_Resolution -> Color(0xFFB3E5FC) // lightblue100
        TslmLabel.File_Video_Encodec -> Color(0xFF81D4FA)
        TslmLabel.File_Video_Bit -> Color(0xFF4FC3F7)
        TslmLabel.File_Video_FrameRate -> Color(0xFF4DD0E1)
        TslmLabel.File_Video_Quality -> Color(0xFF80DEEA)

        // 音频相关（浅紫蓝）
        TslmLabel.File_Audio_Encodec -> Color(0xFFC5CAE9)   // indigo100
        TslmLabel.File_Audio_Channel -> Color(0xFF9FA8DA)
        TslmLabel.File_Audio_Track -> Color(0xFF7986CB)
        TslmLabel.File_Audio_Feature -> Color(0xFF5C6BC0)
    }
}

fun TslmLabel.name(): StringResource {
    return when (this) {
        TslmLabel.Title -> Res.string.tslm_title_name
        TslmLabel.Season -> Res.string.tslm_season_name
        TslmLabel.Episode -> Res.string.tslm_episode_name
        TslmLabel.Producer -> Res.string.tslm_producer_name
        TslmLabel.Year -> Res.string.tslm_year_name
        TslmLabel.Month -> Res.string.tslm_month_name
        TslmLabel.Type -> Res.string.tslm_type_name
        TslmLabel.Release_State -> Res.string.tslm_release_state_name
        TslmLabel.Release_Version -> Res.string.tslm_release_version_name
        TslmLabel.Subtitle_Language -> Res.string.tslm_subtitle_language_name
        TslmLabel.Subtitle_Format -> Res.string.tslm_subtitle_format_name
        TslmLabel.Subtitle_Type -> Res.string.tslm_subtitle_type_name
        TslmLabel.File_Type -> Res.string.tslm_file_type_name
        TslmLabel.File_Source -> Res.string.tslm_file_source_name
        TslmLabel.File_Platform -> Res.string.tslm_file_platform_name
        TslmLabel.File_Video_Resolution -> Res.string.tslm_file_video_resolution_name
        TslmLabel.File_Video_Encodec -> Res.string.tslm_file_video_encodec_name
        TslmLabel.File_Video_Bit -> Res.string.tslm_file_video_bit_name
        TslmLabel.File_Video_FrameRate -> Res.string.tslm_file_video_framerate_name
        TslmLabel.File_Video_Quality -> Res.string.tslm_file_video_quality_name
        TslmLabel.File_Audio_Encodec -> Res.string.tslm_file_audio_encodec_name
        TslmLabel.File_Audio_Channel -> Res.string.tslm_file_audio_channel_name
        TslmLabel.File_Audio_Feature -> Res.string.tslm_file_audio_feature_name
        TslmLabel.File_Audio_Track -> Res.string.tslm_file_audio_track_name
    }
}

fun TslmLabel.desc(): StringResource {
    return when (this) {
        TslmLabel.Title -> Res.string.tslm_title_desc
        TslmLabel.Season -> Res.string.tslm_season_desc
        TslmLabel.Episode -> Res.string.tslm_episode_desc
        TslmLabel.Producer -> Res.string.tslm_producer_desc
        TslmLabel.Year -> Res.string.tslm_year_desc
        TslmLabel.Month -> Res.string.tslm_month_desc
        TslmLabel.Type -> Res.string.tslm_type_desc
        TslmLabel.Release_State -> Res.string.tslm_release_state_desc
        TslmLabel.Release_Version -> Res.string.tslm_release_version_desc
        TslmLabel.Subtitle_Language -> Res.string.tslm_subtitle_language_desc
        TslmLabel.Subtitle_Format -> Res.string.tslm_subtitle_format_desc
        TslmLabel.Subtitle_Type -> Res.string.tslm_subtitle_type_desc
        TslmLabel.File_Type -> Res.string.tslm_file_type_desc
        TslmLabel.File_Source -> Res.string.tslm_file_source_desc
        TslmLabel.File_Platform -> Res.string.tslm_file_platform_desc
        TslmLabel.File_Video_Resolution -> Res.string.tslm_file_video_resolution_desc
        TslmLabel.File_Video_Encodec -> Res.string.tslm_file_video_encodec_desc
        TslmLabel.File_Video_Bit -> Res.string.tslm_file_video_bit_desc
        TslmLabel.File_Video_FrameRate -> Res.string.tslm_file_video_framerate_desc
        TslmLabel.File_Video_Quality -> Res.string.tslm_file_video_quality_desc
        TslmLabel.File_Audio_Encodec -> Res.string.tslm_file_audio_encodec_desc
        TslmLabel.File_Audio_Channel -> Res.string.tslm_file_audio_channel_desc
        TslmLabel.File_Audio_Feature -> Res.string.tslm_file_audio_feature_desc
        TslmLabel.File_Audio_Track -> Res.string.tslm_file_audio_track_desc
    }
}
