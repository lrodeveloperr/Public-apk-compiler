@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package studio.gooduse.kitchenprep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    tr: Translate,
    onTerms: () -> Unit,
    onSafety: () -> Unit,
    onPrivacy: () -> Unit,
    onComplete: () -> Unit,
) {
    var accepted by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        val isLandscape = maxWidth > maxHeight
        val isTablet = maxWidth >= 700.dp && maxHeight >= 700.dp
        val horizontalPadding = when {
            isTablet -> 48.dp
            isLandscape -> 32.dp
            else -> 20.dp
        }
        val maxContentWidth = if (isLandscape) 760.dp else 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = if (isLandscape) 16.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth),
                isTablet = isTablet,
            )

            Spacer(Modifier.height(if (isLandscape) 34.dp else if (isTablet) 88.dp else 70.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth),
                horizontalAlignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tr("onboardingTitle", "Kitchen prep, organized."),
                    fontSize = when {
                        isTablet -> 44.sp
                        isLandscape -> 38.sp
                        else -> 36.sp
                    },
                    lineHeight = when {
                        isTablet -> 48.sp
                        isLandscape -> 42.sp
                        else -> 40.sp
                    },
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = if (isLandscape) TextAlign.Start else TextAlign.Center,
                    maxLines = 3,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tr("onboardingTagline", "Prep with purpose."),
                    fontSize = if (isTablet) 17.sp else 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (isLandscape) TextAlign.Start else TextAlign.Center,
                    maxLines = 2,
                )

                Spacer(Modifier.height(if (isTablet) 32.dp else 24.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = accepted,
                            onCheckedChange = { accepted = it },
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            Text(
                                text = tr("onboardingAccept", "I accept and acknowledge"),
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                CompactLegalLink(tr("terms", "Terms"), onTerms)
                                Text("·", modifier = Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CompactLegalLink(tr("safety", "Safety"), onSafety)
                                Text("·", modifier = Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CompactLegalLink(tr("privacy", "Privacy"), onPrivacy)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onComplete,
                    enabled = accepted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        tr("getStarted", "Get Started"),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = tr("boardsStay", "Data stays on this device."),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BrandRow(modifier: Modifier = Modifier, isTablet: Boolean) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = Modifier
                .size(if (isTablet) 60.dp else 52.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            "Kitchen Prep Board",
            fontSize = if (isTablet) 22.sp else 20.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
        )
    }
}

@Composable
private fun CompactLegalLink(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        modifier = Modifier.heightIn(min = 32.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}
