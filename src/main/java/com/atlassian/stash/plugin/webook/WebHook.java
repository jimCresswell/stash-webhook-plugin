package com.atlassian.stash.plugin.webook;

import com.atlassian.stash.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;

public class WebHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private static final Logger log = LoggerFactory.getLogger(WebHook.class);
	private static final String REFS_HEADS = "refs/heads/";
    private static final String CONFIG_URL = "url";

    private final NavBuilder navBuilder;

	public WebHook(NavBuilder navBuilder) {
		this.navBuilder = navBuilder;
	}

    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
		String url = getUrl(context, getUpdateBranches(refChanges));
		try {
			URLConnection connection = new URL(url).openConnection();
			connection.getInputStream().close();
		} catch (Exception e) {
			log.error("Error triggering: " + url, e);
		}
	}

	private String getUrl(RepositoryHookContext context, Iterable<String> b) {
		String cloneUrl = navBuilder.repo(context.getRepository()).clone(context.getRepository().getScmId()).buildAbsolute();
		String branches = urlEncode(Joiner.on(",").join(b));
		return context.getSettings().getString(CONFIG_URL, "").replace("$URL", urlEncode(cloneUrl)).replace("$BRANCHES", branches);
	}

	private static String urlEncode(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private Iterable<String> getUpdateBranches(Collection<RefChange> refChanges) {
		return Iterables.transform(Iterables.filter(refChanges, new Predicate<RefChange>() {
			@Override
			public boolean apply(RefChange input) {
				// We only care about non-deleted branches
				return input.getType() != RefChangeType.DELETE && input.getRefId().startsWith(REFS_HEADS);
			}
		}), new Function<RefChange, String>() {
			@Override
			public String apply(RefChange input) {
				// Not 100% sure whether this is _just_ branch or is full ref?
				return input.getRefId().replace(REFS_HEADS, "");
			}
		});
	}

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        String url = settings.getString(CONFIG_URL);
        if (url == null || url.trim().isEmpty()) {
            errors.addFieldError(CONFIG_URL, "URL field is blank, please supply one");
        }
    }
}
