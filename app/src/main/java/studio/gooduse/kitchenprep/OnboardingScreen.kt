@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package studio.gooduse.kitchenprep

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
    ) {
        val isLandscape = maxWidth > maxHeight
        val isTablet = maxWidth >= 700.dp && maxHeight >= 700.dp

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isTablet) 56.dp else 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 40.dp else 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OnboardingCopy(
                    modifier = Modifier.weight(0.92f),
                    accepted = accepted,
                    tr = tr,
                    isTablet = isTablet,
                    onAcceptedChange = { accepted = it },
                    onTerms = onTerms,
                    onSafety = onSafety,
                    onPrivacy = onPrivacy,
                    onComplete = onComplete,
                )
                Image(
                    painter = painterResource(R.drawable.onboarding_hero_landscape),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1.08f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(if (isTablet) 28.dp else 22.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isTablet) 48.dp else 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 820.dp),
                    isTablet = isTablet,
                )
                Spacer(Modifier.height(if (isTablet) 28.dp else 18.dp))
                Image(
                    painter = painterResource(R.drawable.onboarding_hero_portrait),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isTablet) 760.dp else 620.dp)
                        .heightIn(min = if (isTablet) 330.dp else 250.dp, max = if (isTablet) 470.dp else 390.dp)
                        .clip(RoundedCornerShape(if (isTablet) 28.dp else 24.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(if (isTablet) 28.dp else 22.dp))
                OnboardingCopy(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    accepted = accepted,
                    tr = tr,
                    isTablet = isTablet,
                    onAcceptedChange = { accepted = it },
                    onTerms = onTerms,
                    onSafety = onSafety,
                    onPrivacy = onPrivacy,
                    onComplete = onComplete,
                    showBrand = false,
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun OnboardingCopy(
    modifier: Modifier,
    accepted: Boolean,
    tr: Translate,
    isTablet: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onTerms: () -> Unit,
    onSafety: () -> Unit,
    onPrivacy: () -> Unit,
    onComplete: () -> Unit,
    showBrand: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (showBrand) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        if (showBrand) {
            BrandRow(modifier = Modifier.fillMaxWidth(), isTablet = isTablet)
            Spacer(Modifier.height(if (isTablet) 42.dp else 24.dp))
        }

        Text(
            text = tr("onboardingTitle", "Kitchen prep, organized."),
            fontSize = if (isTablet) 46.sp else 38.sp,
            lineHeight = if (isTablet) 50.sp else 42.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = if (showBrand) TextAlign.Start else TextAlign.Center,
            maxLines = 3,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = tr("onboardingTagline", "Prep with purpose."),
            fontSize = if (isTablet) 18.sp else 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (showBrand) TextAlign.Start else TextAlign.Center,
            maxLines = 2,
        )
        Spacer(Modifier.height(if (isTablet) 28.dp else 20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = onAcceptedChange,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                ) {
                    Text(
                        tr("onboardingAccept", "I accept and acknowledge"),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        CompactLegalLink(tr("terms", "Terms of Use"), onTerms)
                        Text("·", modifier = Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CompactLegalLink(tr("safety", "Safety Notice"), onSafety)
                        Text("·", modifier = Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CompactLegalLink(tr("privacy", "Privacy Policy"), onPrivacy)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
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
        Spacer(Modifier.height(12.dp))
        Text(
            text = tr("boardsStay", "Data stays on this device."),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        modifier = Modifier.heightIn(min = 32.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}
