package com.atlassian.stash.plugin.webook;

import com.atlassian.event.api.EventListener;
import com.atlassian.stash.event.RepositoryPushEvent;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
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

public class WebHook {

	private static final Logger log = LoggerFactory.getLogger(WebHook.class);
	private static final String REFS_HEADS = "refs/heads/";

	private final NavBuilder navBuilder;
	private String URL = System.getProperty("webhook.url", "http://localhost:8080/jenkins/git/notifyCommit?url=$URL&branches=$BRANCHES");

	public WebHook(NavBuilder navBuilder) {
		this.navBuilder = navBuilder;
	}

	@EventListener
	public void onPushEvent(RepositoryPushEvent event) {
		String url = getUrl(event);
		try {
			URLConnection connection = new URL(url).openConnection();
			connection.getInputStream().close();
		} catch (Exception e) {
			log.error("Error triggering: " + url, e);
		}
	}

	private String getUrl(RepositoryPushEvent event) {
		String cloneUrl = navBuilder.repo(event.getRepository()).clone("git").buildAbsolute();
		String branches = urlEncode(Joiner.on(",").join(getUpdateBranches(event)));
		return URL.replace("$URL", urlEncode(cloneUrl)).replace("$BRANCHES", branches);
	}

	private static String urlEncode(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private Iterable<String> getUpdateBranches(RepositoryPushEvent event) {
		return Iterables.transform(Iterables.filter(event.getRefChanges(), new Predicate<RefChange>() {
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
}
